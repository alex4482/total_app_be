package com.work.total_app.models.tenant;

import com.work.total_app.models.service.Service;

import java.util.Date;

/**
 * DTO for adding/updating an active service in a rental agreement.
 * When serialized, includesInReport is calculated dynamically if not explicitly set.
 */
public record ActiveServiceDto(
    Long serviceId, // ID of the Service definition
    Double customMonthlyCost, // Optional: override default cost
    Boolean includeInReport, // Optional: override default include in report flag (null = use service default)
    Date activeFrom, // Date from which service is active
    Date activeUntil, // Optional: date until which service is active (null = indefinitely)
    String notes // Optional: notes about this service
) {
    /**
     * Convert ActiveService to DTO with resolved includeInReport.
     * If includeInReport is null in ActiveService, uses service.getDefaultIncludeInReport().
     */
    public static ActiveServiceDto fromActiveService(ActiveService activeService, Service service) {
        Boolean includeInReport = activeService.getIncludeInReport();
        if (includeInReport == null) {
            // Use service default if not explicitly set
            includeInReport = service.getDefaultIncludeInReport() != null 
                ? service.getDefaultIncludeInReport() 
                : false;
        }
        
        return new ActiveServiceDto(
            activeService.getServiceId(),
            activeService.getCustomMonthlyCost(),
            includeInReport,
            activeService.getActiveFrom(),
            activeService.getActiveUntil(),
            activeService.getNotes()
        );
    }
}
