package com.work.total_app.models.reading;

import com.work.total_app.models.building.BuildingLocation;

public record IndexCounterFilter (
        CounterType counterType,
        String locationId,
        IndexCounterLocationType locationType,
        BuildingLocation buildingLocation) {
}
