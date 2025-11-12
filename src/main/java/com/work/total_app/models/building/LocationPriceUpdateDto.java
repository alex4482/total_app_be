package com.work.total_app.models.building;

import com.work.total_app.models.reading.CounterType;
import lombok.Data;

/**
 * DTO for updating unit prices at location level.
 */
@Data
public class LocationPriceUpdateDto {
    private CounterType counterType;
    private Double unitPrice;
    private Boolean updateAllCounters; // If true, update defaultUnitPrice on all counters of this type
    private Boolean recalculateAll; // If true, recalculate all costs for readings without local prices
}

