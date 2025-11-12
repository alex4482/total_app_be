package com.work.total_app.models.tenant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Custom value for a service in a specific month.
 * Allows overriding the default calculated value for a service in a specific month.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service_monthly_values", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"rental_data_id", "service_id", "year", "month"})
})
public class ServiceMonthlyValue {
    @Id
    @GeneratedValue
    private Long id;

    /**
     * Rental agreement this value belongs to.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rental_data_id", nullable = false)
    private TenantRentalData rentalData;

    /**
     * Service ID (reference to Service).
     */
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    /**
     * Year for this value.
     */
    @Column(nullable = false)
    private Integer year;

    /**
     * Month (0-11, where 0 = January, 11 = December).
     */
    @Column(nullable = false)
    private Integer month;

    /**
     * Custom value for this service in this month.
     * If null, uses the default calculation (cost or formula).
     */
    private Double customValue;

    /**
     * Whether this value was set manually (true) or is calculated (false).
     * If true, customValue takes priority over calculated value.
     */
    @Column(name = "is_manual", nullable = false)
    private Boolean isManual = false;

    /**
     * Notes or additional information about this value.
     */
    @Column(length = 1000)
    private String notes;

    /**
     * Timestamp when this value was set.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}

