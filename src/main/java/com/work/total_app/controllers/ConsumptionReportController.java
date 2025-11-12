package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.tenant.TenantRentalData;
import com.work.total_app.models.tenant.UpdateFromExcelResult;
import com.work.total_app.repositories.TenantRentalDataRepository;
import com.work.total_app.services.ExcelReportService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Year;

@Controller
@RequestMapping("/consumption-reports")
@Log4j2
public class ConsumptionReportController {

    @Autowired
    private ExcelReportService excelReportService;
    
    @Autowired
    private TenantRentalDataRepository rentalDataRepository;

    /**
     * Generate and download Excel report for a specific rental agreement and year.
     * Endpoint: GET /consumption-reports/rental/{rentalAgreementId}/year/{year}
     */
    @GetMapping("/rental/{rentalAgreementId}/year/{year}")
    @ResponseBody
    public ResponseEntity<?> generateYearlyReport(
            @PathVariable Long rentalAgreementId,
            @PathVariable int year) {
        
        try {
            TenantRentalData rentalData = rentalDataRepository.findById(rentalAgreementId)
                .orElseThrow(() -> new RuntimeException("Rental agreement not found"));
            
            byte[] excelBytes = excelReportService.generateConsumptionReport(rentalAgreementId, year);
            
            String tenantName = rentalData.getTenant().getName();
            String spaceName = rentalData.getRentalSpace().getName();
            String filename = sanitizeFilename(tenantName + "-" + spaceName + "-" + year + ".xlsx");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                URLEncoder.encode(filename, StandardCharsets.UTF_8));
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(excelBytes));
                
        } catch (IOException e) {
            log.error("Error generating Excel report for rental agreement {}", rentalAgreementId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to generate Excel report: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error generating report", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * Generate multi-year report with one sheet per year.
     * Endpoint: GET /consumption-reports/rental/{rentalAgreementId}/years?start={startYear}&end={endYear}
     */
    @GetMapping("/rental/{rentalAgreementId}/years")
    @ResponseBody
    public ResponseEntity<?> generateMultiYearReport(
            @PathVariable Long rentalAgreementId,
            @RequestParam(required = false) Integer start,
            @RequestParam(required = false) Integer end) {
        
        try {
            TenantRentalData rentalData = rentalDataRepository.findById(rentalAgreementId)
                .orElseThrow(() -> new RuntimeException("Rental agreement not found"));
            
            // Default to current year if not specified
            int currentYear = Year.now().getValue();
            int startYear = (start != null) ? start : currentYear;
            int endYear = (end != null) ? end : currentYear;
            
            if (startYear > endYear) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Start year cannot be after end year"));
            }
            
            byte[] excelBytes = excelReportService.generateMultiYearReport(rentalAgreementId, startYear, endYear);
            
            String tenantName = rentalData.getTenant().getName();
            String spaceName = rentalData.getRentalSpace().getName();
            String filename = sanitizeFilename(tenantName + "-" + spaceName + "-" + startYear + "-" + endYear + ".xlsx");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                URLEncoder.encode(filename, StandardCharsets.UTF_8));
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(excelBytes));
                
        } catch (IOException e) {
            log.error("Error generating multi-year Excel report for rental agreement {}", rentalAgreementId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to generate Excel report: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error generating multi-year report", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * Generate reports for all active rental agreements for a specific year.
     * Returns a list of generated reports or downloads a ZIP file.
     * Endpoint: GET /consumption-reports/all-active/year/{year}
     */
    @GetMapping("/all-active/year/{year}")
    @ResponseBody
    public ResponseEntity<?> generateReportsForAllActiveRentals(@PathVariable int year) {
        try {
            // Find all active rental agreements
            var activeRentals = rentalDataRepository.findAll().stream()
                .filter(r -> r.getEndDate() == null || r.getEndDate().after(new java.util.Date()))
                .toList();
            
            if (activeRentals.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No active rental agreements found", null));
            }
            
            log.info("Generating {} reports for year {}", activeRentals.size(), year);
            
            // For now, return success message. Can be extended to generate ZIP with all reports
            return ResponseEntity.ok(
                ApiResponse.success(
                    String.format("Found %d active rental agreements", activeRentals.size()),
                    activeRentals.stream()
                        .map(r -> new ReportInfo(
                            r.getId(),
                            r.getTenant().getName(),
                            r.getRentalSpace().getName()
                        ))
                        .toList()
                )
            );
            
        } catch (Exception e) {
            log.error("Error generating reports for all active rentals", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to generate reports: " + e.getMessage()));
        }
    }

    /**
     * Update service values from modified Excel file.
     * Endpoint: POST /consumption-reports/rental/{rentalAgreementId}/year/{year}/update
     * 
     * Compares values from Excel with calculated values and updates ServiceMonthlyValue for differences.
     */
    @PostMapping("/rental/{rentalAgreementId}/year/{year}/update")
    @ResponseBody
    public ResponseEntity<ApiResponse<UpdateFromExcelResult>> updateFromExcel(
            @PathVariable Long rentalAgreementId,
            @PathVariable int year,
            @RequestParam("file") MultipartFile file) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Excel file is required"));
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File must be an Excel file (.xlsx or .xls)"));
            }
            
            log.info("Updating service values from Excel for rental agreement {} year {}", rentalAgreementId, year);
            
            UpdateFromExcelResult result = excelReportService.updateServiceValuesFromExcel(
                rentalAgreementId, year, file);
            
            return ResponseEntity.ok(ApiResponse.success(
                String.format("Updated %d service values", result.getUpdatedCount()),
                result));
                
        } catch (RuntimeException e) {
            log.error("Error updating from Excel for rental agreement {}", rentalAgreementId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update from Excel: " + e.getMessage()));
        } catch (IOException e) {
            log.error("Error reading Excel file", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to read Excel file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating from Excel", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }

    private String sanitizeFilename(String filename) {
        // Remove or replace characters that are invalid in filenames
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    // Helper DTO for listing available reports
    private record ReportInfo(Long rentalAgreementId, String tenantName, String spaceName) {}
}

