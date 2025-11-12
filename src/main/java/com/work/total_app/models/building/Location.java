package com.work.total_app.models.building;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.work.total_app.models.Observation;
import com.work.total_app.models.reading.CounterType;
import com.work.total_app.models.reading.CounterTypePrice;
import com.work.total_app.models.reading.IndexCounter;
import com.work.total_app.models.reading.LocationType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED) // or SINGLE_TABLE
public abstract class Location {
    @Id
    @GeneratedValue
    protected Long id;

    @Column(unique = true, nullable = false)
    protected String name;
    protected String officialName; // in registre
    @ElementCollection(fetch = FetchType.EAGER)
    protected List<Observation> observations;
    protected BuildingLocation location;
    protected Integer mp;
    protected LocationType type;

    // location → INDEX COUNTERS (inverse side, counters at room level)
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    @JsonManagedReference("location-counters") // Prevent circular reference with IndexCounter.location
    private List<IndexCounter> counters;

    // Default unit prices per counter type for this location
    // All counters of the same type in this location share these prices
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "location_counter_prices", joinColumns = @JoinColumn(name = "location_id"))
    private List<CounterTypePrice> counterTypePrices = new ArrayList<>();

    public void addCounter(IndexCounter ic) {
        counters.add(ic);
        ic.setLocation(this);
    }
    public void removeCounter(IndexCounter ic) {
        counters.remove(ic);
        ic.setLocation(null);
    }

    /**
     * Get the default unit price for a specific counter type at this location.
     */
    public Double getDefaultUnitPriceForType(CounterType counterType) {
        if (counterTypePrices == null) {
            return null;
        }
        return counterTypePrices.stream()
            .filter(p -> p.getCounterType() == counterType)
            .findFirst()
            .map(CounterTypePrice::getUnitPrice)
            .orElse(null);
    }

    /**
     * Set or update the default unit price for a specific counter type at this location.
     */
    public void setDefaultUnitPriceForType(CounterType counterType, Double unitPrice) {
        if (counterTypePrices == null) {
            counterTypePrices = new ArrayList<>();
        }
        
        // Find existing price for this type
        CounterTypePrice existing = counterTypePrices.stream()
            .filter(p -> p.getCounterType() == counterType)
            .findFirst()
            .orElse(null);
        
        if (existing != null) {
            existing.setUnitPrice(unitPrice);
        } else {
            counterTypePrices.add(new CounterTypePrice(counterType, unitPrice));
        }
    }

    /**
     * Populate this location instance with data from DTO.
     */
    protected void populateFromDto(CreateLocationDto dto) {
        this.location = dto.getLocation();
        this.mp = dto.getMp();
        this.name = dto.getName();
        this.observations = dto.getObservations();
        this.type = dto.getType();
        this.officialName = dto.getOfficialName();
    }
}