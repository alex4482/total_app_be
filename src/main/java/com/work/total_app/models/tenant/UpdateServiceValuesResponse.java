package com.work.total_app.models.tenant;

import java.util.List;

/**
 * Response for updating service values.
 */
public record UpdateServiceValuesResponse(
    Integer year,
    Integer month,
    int updatedCount,
    List<UpdatedServiceInfo> updatedServices
) {
    public record UpdatedServiceInfo(
        Long serviceId,
        String serviceName,
        Double oldValue,
        Double newValue
    ) {}
}

