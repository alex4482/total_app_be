package com.work.total_app.models.tenant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Active service for a rental agreement with its specific configuration.
 * Links a Service to a TenantRentalData with custom values.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveService {
    /**
     * ID of the Service definition.
     * Service will be loaded when needed.
     */
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    /**
     * Custom monthly cost for this rental agreement.
     * If null, uses service.defaultMonthlyCost or calculates from formula.
     */
    private Double customMonthlyCost;

    /**
     * Whether this service should be included in consumption reports for this rental agreement.
     * If null, uses service.defaultIncludeInReport.
     */
    private Boolean includeInReport;

    /**
     * Date from which this service is active for this rental agreement.
     */
    @Temporal(TemporalType.DATE)
    private Date activeFrom;

    /**
     * Date until which this service is active (null = active indefinitely).
     */
    @Temporal(TemporalType.DATE)
    private Date activeUntil;

    /**
     * Notes or additional information about this service for this rental agreement.
     */
    @Column(length = 1000)
    private String notes;
}

