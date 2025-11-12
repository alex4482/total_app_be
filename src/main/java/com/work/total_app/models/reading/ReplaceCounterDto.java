package com.work.total_app.models.reading;

import com.work.total_app.models.building.BuildingLocation;
import lombok.Data;

import java.util.Date;

/**
 * DTO for replacing a counter.
 * Contains information about the old counter's final reading and new counter's initial reading.
 */
@Data
public class ReplaceCounterDto {
    // Old counter info
    private Long oldCounterId;              // Required: ID-ul contorului vechi
    private Double oldCounterFinalIndex;    // Required: Ultima citire a contorului vechi
    private Date replacementDate;           // Required: Data înlocuirii
    
    // New counter info
    private String newCounterName;          // Required: Nume contor nou
    private Double newCounterInitialIndex;  // Required: Prima citire a contorului nou
    private CounterType counterType;        // Required: Tip contor (trebuie să fie același)
    private LocationType locationType;      // Optional: dacă lipsește, se ia de la contorul vechi
    private BuildingLocation buildingLocation; // Optional: dacă lipsește, se ia de la contorul vechi
    private Double defaultUnitPrice;        // Optional: preț default pentru contorul nou
}

