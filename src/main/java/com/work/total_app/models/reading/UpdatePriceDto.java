package com.work.total_app.models.reading;

import lombok.Data;

/**
 * DTO for updating unit prices at counter or reading level.
 */
@Data
public class UpdatePriceDto {
    private Double defaultUnitPrice; // For updating counter's global price
    private Double unitPrice; // For updating specific reading's local price
    private Boolean recalculateAll; // If true, recalculate all costs for this counter
}

