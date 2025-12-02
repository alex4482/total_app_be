package com.work.total_app.models.building;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@DiscriminatorValue("Building")
public class Building extends Location {
    // BUILDING → ROOMS (inverse side)
    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
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

    /**
     * Create a Building instance from DTO.
     */
    public static Building fromDto(CreateLocationDto dto) {
        Building building = new Building();
        building.populateFromDto(dto);
        return building;
    }
}

