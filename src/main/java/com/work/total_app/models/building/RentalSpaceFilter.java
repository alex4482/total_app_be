package com.work.total_app.models.building;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.work.total_app.models.Observation;
import com.work.total_app.models.reading.CounterType;
import com.work.total_app.models.reading.IndexCounterLocationType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.List;

public record RentalSpaceFilter(
        String buildingId,
        BuildingLocation buildingLocation,
        Boolean groundLevel,
        Boolean empty,
        String tenantId
        ) { }
