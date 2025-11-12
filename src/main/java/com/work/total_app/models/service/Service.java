package com.work.total_app.models.service;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * General service definition (not tied to any rental agreement).
 * Can be reused across multiple rental agreements.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "Salubrizare", "Alarma", "Întreținere", etc.

    private String description; // Optional description

    // Unit of measure (e.g., "lei", "mc", "kw", etc.)
    private String unitOfMeasure;

    // Default monthly cost (can be overridden per rental agreement)
    private Double defaultMonthlyCost;

    // Formula for calculating service value (optional)
    // If formula is provided, it will be used instead of defaultMonthlyCost
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "formula_id")
    private ServiceFormula formula;

    // Default flag for including this service in consumption reports
    // Can be overridden per rental agreement
    private Boolean defaultIncludeInReport = false;

    // Active/inactive flag (soft delete)
    private Boolean active = true;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}

