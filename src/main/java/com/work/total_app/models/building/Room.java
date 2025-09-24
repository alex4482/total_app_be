package com.work.total_app.models.building;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.work.total_app.models.Observation;
import com.work.total_app.models.reading.IndexCounter;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Room {
    @Id
    private String name;
    private String officialName; // in registre
    @ElementCollection
    private List<Observation> observations;

    // ROOM → BUILDING (owning side)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "building_id")
    @JsonBackReference
    private Building building;

    private Integer mp; // metri patrati
    private Boolean groundLevel;

    // ROOM → INDEX COUNTERS (inverse side, counters at room level)
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndexCounter> counters;
}
