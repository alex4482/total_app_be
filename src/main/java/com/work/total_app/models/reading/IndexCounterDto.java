package com.work.total_app.models.reading;

import com.work.total_app.models.building.BuildingLocation;
import lombok.Data;

@Data
public class IndexCounterDto
{
    private String name;
    private Long locationId;
    private CounterType counterType;
    private LocationType locationType;
    private BuildingLocation buildingLocation;
    private Double defaultUnitPrice; // Global/default price for this counter
}
