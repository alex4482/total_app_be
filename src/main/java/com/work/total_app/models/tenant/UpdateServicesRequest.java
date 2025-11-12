package com.work.total_app.models.tenant;

import java.util.Date;
import java.util.List;

/**
 * DTO for updating active services for a rental agreement.
 */
public record UpdateServicesRequest(
    List<ServiceUpdateDto> services
) {
    public record ServiceUpdateDto(
        Long serviceId,
        Boolean active,
        Double customMonthlyCost,
        Boolean includeInReport, // true = ON MANUAL, false = OFF MANUAL, null = see useDefaultIncludeInReport
        Boolean useDefaultIncludeInReport, // true = IMPLICIT (use service.defaultIncludeInReport), false/null = use includeInReport value
        Date activeFrom,
        Date activeUntil,
        String notes
    ) {}
}

