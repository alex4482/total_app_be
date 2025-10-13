package com.work.total_app.models;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Observation {
    private String message;
    @Enumerated(EnumType.STRING)
    private ObservationUrgency type;
}

enum ObservationUrgency {
    SIMPLE,
    URGENT,
    TODO,
}
