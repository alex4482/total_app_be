package com.work.total_app.models.building;

public record RentalSpaceFilter(
        String buildingId,
        BuildingLocation buildingLocation,
        Boolean groundLevel,
        Boolean empty,
        String tenantId
        ) { }
