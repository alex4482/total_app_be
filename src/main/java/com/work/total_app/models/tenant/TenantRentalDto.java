package com.work.total_app.models.tenant;

import java.util.Date;

/**
 * DTO for creating a new rental agreement.
 */
public record TenantRentalDto(
    Long tenantId,
    Long rentalSpaceId,
    Date startDate,
    Date endDate,
    Double price,
    Currency currency,
    // Contract information (optional)
    String contractNumber,
    Date contractDate,
    // Active services (optional - list of services to activate for this rental agreement)
    java.util.List<ActiveServiceDto> activeServices
) {
}
