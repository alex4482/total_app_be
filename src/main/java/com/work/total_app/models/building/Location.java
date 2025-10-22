package com.work.total_app.models.building;

import com.work.total_app.models.Observation;
import com.work.total_app.models.reading.IndexCounter;
import com.work.total_app.models.reading.LocationType;
import jakarta.persistence.*;
import lombok.Data;

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
    private List<IndexCounter> counters;

    public void addCounter(IndexCounter ic) {
        counters.add(ic);
        ic.setLocation(this);
    }
    public void removeCounter(IndexCounter ic) {
        counters.remove(ic);
        ic.setLocation(null);
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