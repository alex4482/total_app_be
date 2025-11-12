package com.work.total_app.models.service;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Formula for calculating service value.
 * Supports: addition, subtraction, multiplication, division, and combinations.
 * 
 * Example formulas:
 * - "rent * 0.03" (3% of rent)
 * - "waterConsumption * 0.5 + 20" (50% of water consumption + 20)
 * - "(waterConsumption + gasConsumption) * 0.1" (10% of water + gas)
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceFormula {
    @Id
    @GeneratedValue
    private Long id;

    /**
     * Formula expression as string.
     * Supported variables:
     * - "rent" - monthly rent
     * - "waterConsumption" - water consumption for the month
     * - "gasConsumption" - gas consumption for the month
     * - "electricityConsumption220V" - electricity consumption 220V for the month
     * - "electricityConsumption380V" - electricity consumption 380V for the month
     * - "costConsumApa" - cost of water consumption (consumption * unit price)
     * - "costConsumGaz" - cost of gas consumption (consumption * unit price)
     * - "costConsumCurent220V" - cost of electricity 220V (consumption * unit price)
     * - "costConsumCurent380V" - cost of electricity 380V (consumption * unit price)
     * - "service_1", "service_2", ... - value of other services by ID (e.g., service_5 for service with ID 5)
     * 
     * Supported operators: +, -, *, /, (, )
     * 
     * Examples:
     * - "rent * 0.03" (3% of rent)
     * - "waterConsumption * 0.5 + 20" (50% of water consumption + 20)
     * - "(waterConsumption + gasConsumption) * 0.1" (10% of water + gas)
     * - "costConsumApa * 0.1 + costConsumGaz * 0.05" (10% of water cost + 5% of gas cost)
     * - "service_1 * 0.03 + service_2" (3% of service 1 + value of service 2)
     */
    @Column(nullable = false, length = 500)
    private String expression;

    /**
     * Description of what the formula calculates.
     */
    private String description;
}

