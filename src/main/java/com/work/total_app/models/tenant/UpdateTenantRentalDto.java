package com.work.total_app.models.tenant;

import java.util.Date;

/**
 * DTO for updating rental agreement details.
 */
public record UpdateTenantRentalDto(
    Date startDate,
    Date endDate,
    Double rent,
    Currency currency,
    // Contract information (optional)
    String contractNumber,
    Date contractDate,
    // Active services (optional - replaces all active services)
    java.util.List<ActiveServiceDto> activeServices
) {
}

