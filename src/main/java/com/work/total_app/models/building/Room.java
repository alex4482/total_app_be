package com.work.total_app.models.building;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.work.total_app.models.Observation;
import com.work.total_app.models.reading.IndexCounter;
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
    @JsonBackReference
    private Building building;

    private Boolean groundLevel;
}
