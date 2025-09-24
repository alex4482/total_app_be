package com.work.total_app.models.building;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.work.total_app.models.Observation;
import com.work.total_app.models.reading.IndexCounter;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
public class Building {
    @Id
    private String id;
    private String name;
    private BuildingLocation location;
    @ElementCollection
    private List<Observation> observations;
    private Integer mp;


    // BUILDING → ROOMS (inverse side)
    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Room> rooms = new java.util.ArrayList<>();

    // BUILDING → INDEX COUNTERS (inverse side, counters at building level)
    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndexCounter> buildingCounters = new java.util.ArrayList<>();

    public void addRoom(Room r) {
        rooms.add(r);
        r.setBuilding(this);
    }
    public void removeRoom(Room r) {
        rooms.remove(r);
        r.setBuilding(null);
    }

    public void createId() {
        id = location.name() + "-" + name;
    }
}

