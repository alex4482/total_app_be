package com.work.total_app.models.tenant;

import java.util.Date;

/**
 * DTO for updating rental agreement details.
 */
public record UpdateTenantRentalDto(
    Date startDate,
    Date endDate,
    Double rent,
    Currency currency
) {
}

