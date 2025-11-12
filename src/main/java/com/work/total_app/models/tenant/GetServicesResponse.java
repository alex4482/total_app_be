package com.work.total_app.models.tenant;

import java.util.List;

/**
 * Response for getting services for a rental agreement.
 */
public record GetServicesResponse(
    Long rentalAgreementId,
    List<ServiceWithResolvedIncludeInReport> services
) {}

