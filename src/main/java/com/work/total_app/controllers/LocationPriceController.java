package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.building.Location;
import com.work.total_app.models.building.LocationPriceUpdateDto;
import com.work.total_app.models.reading.CounterType;
import com.work.total_app.services.LocationPriceService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/locations")
@Log4j2
public class LocationPriceController {

    @Autowired
    private LocationPriceService locationPriceService;

    /**
     * Update the default unit price for a counter type at a location.
     * This affects all counters of that type in the location.
     * 
     * Endpoint: PATCH /locations/{locationId}/prices
     * Body: {
     *   "counterType": "WATER",
     *   "unitPrice": 15.50,
     *   "updateAllCounters": true,
     *   "recalculateAll": true
     * }
     */
    @PatchMapping("/{locationId}/prices")
    @ResponseBody
    public ResponseEntity<ApiResponse<Location>> updateLocationPrice(
            @PathVariable Long locationId,
            @RequestBody LocationPriceUpdateDto dto) {
        
        try {
            if (dto.getCounterType() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("counterType is required"));
            }
            if (dto.getUnitPrice() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("unitPrice is required"));
            }
            
            boolean updateCounters = dto.getUpdateAllCounters() != null && dto.getUpdateAllCounters();
            boolean recalculate = dto.getRecalculateAll() != null && dto.getRecalculateAll();
            
            Location updated = locationPriceService.updateLocationPrice(
                locationId, 
                dto.getCounterType(), 
                dto.getUnitPrice(),
                updateCounters,
                recalculate
            );
            
            String message = buildSuccessMessage(dto.getCounterType(), updateCounters, recalculate);
            return ResponseEntity.ok(ApiResponse.success(message, updated));
            
        } catch (Exception e) {
            log.error("Error updating location price", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to update price: " + e.getMessage()));
        }
    }

    /**
     * Get the default unit price for a specific counter type at a location.
     * 
     * Endpoint: GET /locations/{locationId}/prices?counterType=WATER
     */
    @GetMapping("/{locationId}/prices")
    @ResponseBody
    public ResponseEntity<ApiResponse<Double>> getLocationPrice(
            @PathVariable Long locationId,
            @RequestParam CounterType counterType) {
        
        try {
            Double price = locationPriceService.getLocationPrice(locationId, counterType);
            
            if (price == null) {
                return ResponseEntity.ok(
                    ApiResponse.success("No price configured for " + counterType + " at this location", null)
                );
            }
            
            return ResponseEntity.ok(ApiResponse.success(price));
            
        } catch (Exception e) {
            log.error("Error getting location price", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get price: " + e.getMessage()));
        }
    }

    /**
     * Get all price configurations for a location.
     * 
     * Endpoint: GET /locations/{locationId}/all-prices
     */
    @GetMapping("/{locationId}/all-prices")
    @ResponseBody
    public ResponseEntity<ApiResponse<Location>> getAllLocationPrices(@PathVariable Long locationId) {
        try {
            Location location = locationPriceService.getLocationWithPrices(locationId);
            return ResponseEntity.ok(ApiResponse.success(location));
        } catch (Exception e) {
            log.error("Error getting location prices", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to get prices: " + e.getMessage()));
        }
    }

    private String buildSuccessMessage(CounterType counterType, boolean updateCounters, boolean recalculate) {
        StringBuilder msg = new StringBuilder("Price updated for " + counterType);
        
        if (updateCounters && recalculate) {
            msg.append(", all counter prices updated and costs recalculated");
        } else if (updateCounters) {
            msg.append(", all counter prices updated");
        } else if (recalculate) {
            msg.append(", costs recalculated");
        }
        
        return msg.toString();
    }
}

