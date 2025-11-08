package com.work.total_app.models.tenant;

import java.util.Date;

/**
 * DTO for changing rental price with effective date.
 */
public record ChangePriceDto(
    Double newPrice,
    Date effectiveDate
) {
}

