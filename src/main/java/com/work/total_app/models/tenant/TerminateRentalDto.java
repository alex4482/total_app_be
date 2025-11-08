package com.work.total_app.models.tenant;

import java.util.Date;

/**
 * DTO for terminating a rental agreement.
 */
public record TerminateRentalDto(
    Date endDate
) {
}

