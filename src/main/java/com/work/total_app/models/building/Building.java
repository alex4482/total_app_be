package com.work.total_app.models.building;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.work.total_app.models.Observation;
import com.work.total_app.models.reading.IndexCounter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class Building extends Location {
    // BUILDING → ROOMS (inverse side)
    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Room> rooms = new java.util.ArrayList<>();

    public void addRoom(Room r) {
        rooms.add(r);
        r.setBuilding(this);
    }
    public void removeRoom(Room r) {
        rooms.remove(r);
        r.setBuilding(null);
    }
}

