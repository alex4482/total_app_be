package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.tenant.*;
import com.work.total_app.services.TenantRentalService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tenant-rental-data")
@Log4j2
public class TenantRentalDataController {

    @Autowired
    private TenantRentalService tenantRentalService;

    /**
     * Update active services for a rental agreement.
     * Endpoint: PUT /tenant-rental-data/{rentalAgreementId}/services
     * 
     * Returns updated services with resolved includeInReport values and includeInReportMode.
     */
    @PutMapping("/{rentalAgreementId}/services")
    @ResponseBody
    public ResponseEntity<ApiResponse<GetServicesResponse>> updateServices(
            @PathVariable Long rentalAgreementId,
            @RequestBody UpdateServicesRequest request) {
        try {
            GetServicesResponse response = tenantRentalService.updateServicesAndGet(rentalAgreementId, request);
            return ResponseEntity.ok(ApiResponse.success("Serviciile au fost actualizate cu succes", response));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating services for rental agreement {}", rentalAgreementId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update services: " + e.getMessage()));
        }
    }

    /**
     * Update service values for a specific month.
     * Endpoint: PUT /tenant-rental-data/{rentalAgreementId}/service-values
     */
    @PutMapping("/{rentalAgreementId}/service-values")
    @ResponseBody
    public ResponseEntity<ApiResponse<UpdateServiceValuesResponse>> updateServiceValues(
            @PathVariable Long rentalAgreementId,
            @RequestBody UpdateServiceValuesRequest request) {
        try {
            UpdateServiceValuesResponse response = tenantRentalService.updateServiceValues(rentalAgreementId, request);
            return ResponseEntity.ok(ApiResponse.success("Valorile serviciilor au fost actualizate cu succes", response));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating service values for rental agreement {}", rentalAgreementId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update service values: " + e.getMessage()));
        }
    }

    /**
     * Get services for a rental agreement with resolved includeInReport values.
     * Endpoint: GET /tenant-rental-data/{rentalAgreementId}/services
     * 
     * Returns services with includeInReport calculated from service defaults if not explicitly set.
     */
    @GetMapping("/{rentalAgreementId}/services")
    @ResponseBody
    public ResponseEntity<ApiResponse<GetServicesResponse>> getServices(
            @PathVariable Long rentalAgreementId) {
        try {
            GetServicesResponse response = tenantRentalService.getServices(rentalAgreementId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting services for rental agreement {}", rentalAgreementId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get services: " + e.getMessage()));
        }
    }

    /**
     * Get service values for a specific month.
     * Endpoint: GET /tenant-rental-data/{rentalAgreementId}/service-values?year={year}&month={month}
     */
    @GetMapping("/{rentalAgreementId}/service-values")
    @ResponseBody
    public ResponseEntity<ApiResponse<GetServiceValuesResponse>> getServiceValues(
            @PathVariable Long rentalAgreementId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        try {
            if (year == null || month == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Year and month are required"));
            }
            GetServiceValuesResponse response = tenantRentalService.getServiceValues(rentalAgreementId, year, month);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting service values for rental agreement {}", rentalAgreementId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get service values: " + e.getMessage()));
        }
    }
}
