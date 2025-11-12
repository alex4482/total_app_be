package com.work.total_app.models.service;

import lombok.Data;

/**
 * DTO for creating/updating a Service.
 */
@Data
public class ServiceDto {
    private String name;
    private String description;
    private String unitOfMeasure;
    private Double defaultMonthlyCost;
    private ServiceFormulaDto formula;
    private Boolean defaultIncludeInReport;
    private Boolean active;
}

