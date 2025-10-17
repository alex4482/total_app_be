package com.work.total_app.models.tenant;

import java.util.Date;

public record TenantRentalDto(Long tenantId,
                              Long rentalSpaceId,
                                Date startDate,
                                Double price)
{ }
