package com.work.total_app.models.tenant;

import java.util.List;

/**
 * DTO for updating service values for a specific month.
 */
public record UpdateServiceValuesRequest(
    Integer year,
    Integer month, // 1-12
    List<ServiceValueDto> serviceValues
) {
    public record ServiceValueDto(
        Long serviceId,
        Double value,
        String notes
    ) {}
}

