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
    protected String id;
    protected String name;
    protected String officialName; // in registre
    @ElementCollection
    protected List<Observation> observations;
    protected BuildingLocation location;
    protected Integer mp;
    protected LocationType type;

    // location → INDEX COUNTERS (inverse side, counters at room level)
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndexCounter> counters;

    public void createId() {
        id = location.name() + "-" + name;
    }

    public void addCounter(IndexCounter ic) {
        counters.add(ic);
        ic.setLocation(this);
    }
    public void removeCounter(IndexCounter ic) {
        counters.remove(ic);
        ic.setLocation(null);
    }
}