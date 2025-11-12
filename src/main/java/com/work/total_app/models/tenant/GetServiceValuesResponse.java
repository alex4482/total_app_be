package com.work.total_app.models.tenant;

import java.util.List;

/**
 * Response for getting service values for a specific month.
 */
public record GetServiceValuesResponse(
    Integer year,
    Integer month,
    List<ServiceValueInfo> serviceValues
) {
    public record ServiceValueInfo(
        Long serviceId,
        String serviceName,
        Double value,
        Double calculatedValue,
        Boolean isManual,
        String notes,
        String unitOfMeasure
    ) {}
}

