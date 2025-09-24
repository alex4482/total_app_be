package com.work.total_app.models.building;

import com.work.total_app.models.Observation;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Room {
    @Id
    private String name;
    private String officialName; // in registre
    @ElementCollection
    private List<Observation> observations;
    private String buildingId;
    private Integer mp; //metri patrati

    private List<Long> counterIds;
    private List<Long> indexDataIds;
}
