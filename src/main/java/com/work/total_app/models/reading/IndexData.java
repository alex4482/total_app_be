package com.work.total_app.models.reading;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class IndexData {
    @Id
    @GeneratedValue
    private Long id;

    private Double index;
    private Double consumption;

    @Enumerated(EnumType.STRING)
    private CounterType type;

    @Temporal(TemporalType.DATE)
    private Date readingDate;

    // Price/cost fields
    private Double unitPrice; // Local/override price per unit (optional - if null, uses counter's defaultUnitPrice)
    private Double totalCost; // Total cost for this reading period (consumption * effectiveUnitPrice)

    // Each IndexData belongs to one counter
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "counter_id")
    @JsonBackReference // Prevent circular reference during JSON serialization
    private IndexCounter counter;

    /**
     * Get the effective unit price to use for calculations.
     * Priority order:
     * 1. Local unitPrice (if set on this reading)
     * 2. Counter's defaultUnitPrice (if set)
     * 3. Location's price for this counter type
     * 4. null (no price available)
     */
    @Transient
    public Double getEffectiveUnitPrice() {
        // 1. Local override price (highest priority)
        if (unitPrice != null) {
            return unitPrice;
        }
        
        // 2. Counter's default price (medium priority)
        if (counter != null && counter.getDefaultUnitPrice() != null) {
            return counter.getDefaultUnitPrice();
        }
        
        // 3. Location's price for this counter type (lowest priority / default)
        if (counter != null && counter.getLocation() != null && counter.getCounterType() != null) {
            Double locationPrice = counter.getLocation().getDefaultUnitPriceForType(counter.getCounterType());
            if (locationPrice != null) {
                return locationPrice;
            }
        }
        
        // 4. No price available
        return null;
    }

    /**
     * JSON property to show the effective price being used in API responses.
     */
    @JsonProperty("effectiveUnitPrice")
    @Transient
    public Double getEffectiveUnitPriceForJson() {
        return getEffectiveUnitPrice();
    }
}


