package com.work.total_app.models.building;

import com.work.total_app.models.Observation;
import com.work.total_app.models.reading.LocationType;
import lombok.Data;

import java.util.List;

@Data
public class CreateLocationDto {
    private String name;
    private String officialName;
    private BuildingLocation location;
    private Integer mp;
    private LocationType type;
    private List<Observation> observations;

    // For Room/RentalSpace
    private Boolean groundLevel;
    private Long buildingId;
}

