package com.work.total_app.models.reading;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for consumption statistics aggregated by various dimensions.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsumptionStatistics {
    private String period; // e.g., "2025-01" for monthly, "2025" for yearly
    private Map<String, CounterTypeStats> byCounterType;
    private Map<String, LocationStats> byLocation;
    private Map<String, BuildingStats> byBuilding;
    private Double totalConsumption;
    private Double totalCost;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CounterTypeStats {
        private String counterType;
        private Double totalConsumption;
        private Double totalCost;
        private Integer readingsCount;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LocationStats {
        private String locationId;
        private String locationName;
        private Double totalConsumption;
        private Double totalCost;
        private Map<String, Double> byCounterType;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BuildingStats {
        private Long buildingId;
        private String buildingName;
        private Double totalConsumption;
        private Double totalCost;
        private Map<String, Double> byCounterType;
    }
}

