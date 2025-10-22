package com.work.total_app.models.building;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Room extends Location{

    // ROOM → BUILDING (owning side)
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "building_id")
    @JsonIgnoreProperties("rooms")
    private Building building;

    private Boolean groundLevel;

    /**
     * Create a Room instance from DTO.
     */
    public static Room fromDto(CreateLocationDto dto) {
        Room room = new Room();
        room.populateFromDto(dto);
        room.setGroundLevel(dto.getGroundLevel());
        return room;
    }
}
