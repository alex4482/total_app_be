package com.work.total_app.models.tenant;

import java.util.Date;

public record TenantRentalDto(String tenantId,
        String rentalSpaceId,
        Date startDate,
        Double price)
{ }
