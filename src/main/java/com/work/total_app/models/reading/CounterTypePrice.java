package com.work.total_app.models.reading;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable class to store unit prices per counter type at location level.
 */
@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CounterTypePrice {
    @Enumerated(EnumType.STRING)
    private CounterType counterType;
    
    private Double unitPrice;
}

