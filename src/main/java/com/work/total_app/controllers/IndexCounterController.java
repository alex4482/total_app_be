package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.reading.*;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.services.ConsumptionStatisticsService;
import com.work.total_app.services.ExcelReportService;
import com.work.total_app.services.IndexCounterService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/index-counters")
@Log4j2
public class IndexCounterController {

    @Autowired
    private IndexCounterService counterService;
    
    @Autowired
    private ConsumptionStatisticsService statisticsService;
    
    @Autowired
    private ExcelReportService excelReportService;

    @GetMapping
    @ResponseBody
    public List<IndexCounter> listCounters(@RequestParam(required = false) CounterType type,
                                           @RequestParam(required = false) String locationId,
                                           @RequestParam(required = false) LocationType locationType,
                                           @RequestParam(required = false) BuildingLocation buildingLocation)
    {
        return counterService.getCounters(type, locationId, locationType, buildingLocation);
    }

    /**
     * Get all counters for a specific location.
     * Endpoint: GET /index-counters/location/{locationId}
     */
    @GetMapping("/location/{locationId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<IndexCounter>>> getCountersByLocation(@PathVariable Long locationId)
    {
        try {
            List<IndexCounter> counters = counterService.getCounters(null, String.valueOf(locationId), null, null);
            return ResponseEntity.ok(ApiResponse.success(counters));
        } catch (Exception e) {
            log.error("Error getting counters for location {}", locationId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get counters: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IndexCounter>> listCounter(@PathVariable Long id)
    {
        IndexCounter counter = counterService.getCounter(id);
        return ResponseEntity.ok(ApiResponse.success(counter));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IndexCounter>> addCounter(@RequestBody IndexCounterDto icd)
    {
        IndexCounter saved = counterService.saveCounter(icd);
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Counter created successfully", saved));
    }

    /**
     * Update counter details.
     * Endpoint: PUT /index-counters/{id}
     * Body: {
     *   "name": "Contor Apa Camera 101",
     *   "counterType": "WATER",
     *   "locationType": "ROOM",
     *   "buildingLocation": "LETCANI",
     *   "locationId": 202,
     *   "defaultUnitPrice": 12.50
     * }
     * All fields are optional - only provided fields will be updated.
     */
    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<IndexCounter>> updateCounter(
            @PathVariable Long id,
            @RequestBody IndexCounterDto dto)
    {
        try {
            IndexCounter updated = counterService.updateCounter(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Counter updated successfully", updated));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating counter {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to update counter: " + e.getMessage()));
        }
    }

    @PostMapping("/data")
    public ResponseEntity<ApiResponse<IndexData>> addData(@RequestBody IndexDataDto iData)
    {
        IndexData data = counterService.addData(iData);
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Index data added successfully", data));
    }

    /**
     * Get consumption statistics for a period.
     * Endpoint: GET /index-counters/statistics?startDate=2025-01-01&endDate=2025-12-31&buildingLocation=LETCANI
     */
    @GetMapping("/statistics")
    @ResponseBody
    public ResponseEntity<ApiResponse<ConsumptionStatistics>> getStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) BuildingLocation buildingLocation) {
        
        try {
            ConsumptionStatistics stats = statisticsService.getStatistics(startDate, endDate, buildingLocation);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Error getting consumption statistics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get statistics: " + e.getMessage()));
        }
    }

    /**
     * Get statistics by counter type for a period.
     * Endpoint: GET /index-counters/statistics/by-type?startDate=2025-01-01&endDate=2025-12-31&counterType=WATER
     */
    @GetMapping("/statistics/by-type")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, ConsumptionStatistics.CounterTypeStats>>> getStatsByType(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) CounterType counterType) {
        
        try {
            Map<String, ConsumptionStatistics.CounterTypeStats> stats = 
                statisticsService.getStatsByCounterType(startDate, endDate, counterType);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Error getting statistics by type", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get statistics: " + e.getMessage()));
        }
    }

    /**
     * Update the default (global) unit price for a counter.
     * Endpoint: PATCH /index-counters/{id}/default-price
     * Body: { "defaultUnitPrice": 15.50, "recalculateAll": true }
     */
    @PatchMapping("/{id}/default-price")
    @ResponseBody
    public ResponseEntity<ApiResponse<IndexCounter>> updateDefaultPrice(
            @PathVariable Long id,
            @RequestBody UpdatePriceDto dto) {
        
        try {
            if (dto.getDefaultUnitPrice() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("defaultUnitPrice is required"));
            }
            
            boolean recalculate = dto.getRecalculateAll() != null && dto.getRecalculateAll();
            IndexCounter updated = counterService.updateDefaultUnitPrice(id, dto.getDefaultUnitPrice(), recalculate);
            
            String message = recalculate 
                ? "Default price updated and all costs recalculated"
                : "Default price updated successfully";
            
            return ResponseEntity.ok(ApiResponse.success(message, updated));
        } catch (Exception e) {
            log.error("Error updating default price for counter {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to update price: " + e.getMessage()));
        }
    }

    /**
     * Update the local (override) unit price for a specific reading.
     * Endpoint: PATCH /index-counters/data/{readingId}/price
     * Body: { "unitPrice": 16.00 }
     */
    @PatchMapping("/data/{readingId}/price")
    @ResponseBody
    public ResponseEntity<ApiResponse<IndexData>> updateReadingPrice(
            @PathVariable Long readingId,
            @RequestBody UpdatePriceDto dto) {
        
        try {
            IndexData updated = counterService.updateReadingUnitPrice(readingId, dto.getUnitPrice());
            
            String message = dto.getUnitPrice() == null 
                ? "Local price cleared - now using default price"
                : "Local price updated successfully";
            
            return ResponseEntity.ok(ApiResponse.success(message, updated));
        } catch (Exception e) {
            log.error("Error updating price for reading {}", readingId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to update price: " + e.getMessage()));
        }
    }

    /**
     * Replace an old counter with a new one.
     * This will:
     * 1. Add a final reading to the old counter
     * 2. Create a new counter with the same characteristics
     * 3. Link the new counter's first reading to the old counter's final reading
     * 
     * Endpoint: POST /index-counters/replace
     * Body: {
     *   "oldCounterId": 123,
     *   "oldCounterFinalIndex": 1500.5,
     *   "newCounterName": "Contor Nou Apa",
     *   "newCounterInitialIndex": 0.0,
     *   "replacementDate": "2025-11-09",
     *   "counterType": "WATER",         // Optional: defaults to old counter's type
     *   "locationType": "ROOM",         // Optional: defaults to old counter's location type
     *   "buildingLocation": "LETCANI",  // Optional: defaults to old counter's building location
     *   "defaultUnitPrice": 5.0         // Optional: defaults to old counter's default price
     * }
     */
    @PostMapping("/replace")
    @ResponseBody
    public ResponseEntity<ApiResponse<IndexCounter>> replaceCounter(@RequestBody ReplaceCounterDto dto) {
        try {
            // Validation
            if (dto.getOldCounterId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("oldCounterId is required"));
            }
            if (dto.getOldCounterFinalIndex() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("oldCounterFinalIndex is required"));
            }
            if (dto.getNewCounterName() == null || dto.getNewCounterName().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("newCounterName is required"));
            }
            if (dto.getNewCounterInitialIndex() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("newCounterInitialIndex is required"));
            }
            if (dto.getReplacementDate() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("replacementDate is required"));
            }
            
            IndexCounter newCounter = counterService.replaceCounter(dto);
            
            return ResponseEntity.status(201)
                .body(ApiResponse.success("Counter replaced successfully", newCounter));
        } catch (Exception e) {
            log.error("Error replacing counter", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to replace counter: " + e.getMessage()));
        }
    }

    /**
     * Generate Excel report with counters history.
     * Creates separate sheets per counter type per year.
     * Example sheets: "CURENT 2025", "APA 2025", "GAZ 2025"
     * 
     * Endpoint: GET /index-counters/history-excel
     * 
     * Query Parameters:
     * - year (required): Starting year
     * - endYear (optional): Ending year (if not provided, only startYear is used)
     * - locationId (optional): Filter by specific location ID
     * - buildingLocation (optional): Filter by building location (LETCANI, TOMESTI)
     * - counterType (optional): Filter by counter type (WATER, GAS, ELECTRICITY_220, ELECTRICITY_380)
     * 
     * Examples:
     * - /index-counters/history-excel?year=2025 (all counters for 2025)
     * - /index-counters/history-excel?year=2024&endYear=2025 (all counters for 2024-2025)
     * - /index-counters/history-excel?year=2025&locationId=202 (only location 202)
     * - /index-counters/history-excel?year=2025&buildingLocation=LETCANI (only LETCANI building)
     * 
     * Response: Excel file (application/octet-stream)
     */
    @GetMapping("/history-excel")
    @ResponseBody
    public ResponseEntity<byte[]> getCountersHistoryExcel(
            @RequestParam int year,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) BuildingLocation buildingLocation,
            @RequestParam(required = false) CounterType counterType)
    {
        try {
            // Get counters based on filters
            String locationIdStr = locationId != null ? String.valueOf(locationId) : null;
            List<IndexCounter> counters = counterService.getCounters(counterType, locationIdStr, null, buildingLocation);
            
            if (counters.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            
            // Generate Excel
            byte[] excelBytes = excelReportService.generateCountersHistoryReport(counters, year, endYear);
            
            // Build filename
            String filename = "Istoric_Contori_" + year;
            if (endYear != null && !endYear.equals(year)) {
                filename += "-" + endYear;
            }
            if (buildingLocation != null) {
                filename += "_" + buildingLocation.name();
            }
            filename += ".xlsx";
            
            // Return Excel file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
                
        } catch (Exception e) {
            log.error("Error generating counters history Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
