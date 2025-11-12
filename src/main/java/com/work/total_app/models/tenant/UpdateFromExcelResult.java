package com.work.total_app.models.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of updating service values from Excel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFromExcelResult {
    /**
     * Number of service values updated.
     */
    private int updatedCount;
    
    /**
     * List of updated service monthly values.
     */
    private List<ServiceMonthlyValueInfo> updatedValues = new ArrayList<>();
    
    /**
     * List of errors encountered during update.
     */
    private List<String> errors = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceMonthlyValueInfo {
        private Long serviceId;
        private String serviceName;
        private Integer month;
        private Double oldValue;
        private Double newValue;
    }
}

