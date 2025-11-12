package com.work.total_app.models.tenant;

import java.util.List;

/**
 * Response for updating services.
 */
public record UpdateServicesResponse(
    int updatedCount,
    List<Long> activatedServices,
    List<Long> deactivatedServices
) {}

