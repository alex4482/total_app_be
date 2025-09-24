package com.work.total_app.models.building;

import com.work.total_app.models.Observation;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

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

    private List<Long> counterReadingIds;
    private List<Long> counterIds;

    public void createId() {
        id = location.name() + "-" + name;
    }
}

