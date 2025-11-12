package com.work.total_app.services;

import com.work.total_app.models.building.Location;
import com.work.total_app.models.reading.CounterType;
import com.work.total_app.models.reading.IndexCounter;
import com.work.total_app.models.reading.IndexData;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.repositories.LocationRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Log4j2
public class LocationPriceService {

    @Autowired
    private LocationRepository locationRepository;

    /**
     * Update the default unit price for a specific counter type at a location.
     * This price will be used by all counters of that type in this location
     * (unless they have their own defaultUnitPrice set or readings have local prices).
     * 
     * @param locationId The location ID
     * @param counterType The counter type
     * @param newPrice The new price
     * @param updateAllCounters If true, also update defaultUnitPrice on all counters of this type
     * @param recalculateAll If true, recalculate all costs for readings without local prices
     * @return The updated location
     */
    @Transactional
    public Location updateLocationPrice(Long locationId, CounterType counterType, Double newPrice, 
                                       boolean updateAllCounters, boolean recalculateAll) {
        Location location = locationRepository.findById(locationId)
            .orElseThrow(() -> new NotFoundException("Location not found with id: " + locationId));
        
        // Update the location's price for this counter type
        location.setDefaultUnitPriceForType(counterType, newPrice);
        
        // Get all counters of this type in this location
        List<IndexCounter> countersOfType = location.getCounters().stream()
            .filter(c -> c.getCounterType() == counterType)
            .toList();
        
        if (updateAllCounters) {
            // Update the defaultUnitPrice on each counter
            for (IndexCounter counter : countersOfType) {
                counter.setDefaultUnitPrice(newPrice);
            }
        }
        
        if (recalculateAll) {
            // Recalculate costs for all readings without local prices
            for (IndexCounter counter : countersOfType) {
                for (IndexData reading : counter.getIndexData()) {
                    // Only recalculate if reading doesn't have a local price override
                    if (reading.getUnitPrice() == null && reading.getConsumption() != null) {
                        Double effectivePrice = reading.getEffectiveUnitPrice();
                        if (effectivePrice != null) {
                            reading.setTotalCost(reading.getConsumption() * effectivePrice);
                        }
                    }
                }
            }
        }
        
        Location saved = locationRepository.save(location);
        log.info("Updated price for counter type {} at location {} to {}", 
                counterType, locationId, newPrice);
        
        return saved;
    }

    /**
     * Get the default unit price for a counter type at a specific location.
     */
    public Double getLocationPrice(Long locationId, CounterType counterType) {
        Location location = locationRepository.findById(locationId)
            .orElseThrow(() -> new NotFoundException("Location not found with id: " + locationId));
        
        return location.getDefaultUnitPriceForType(counterType);
    }

    /**
     * Get all prices configured for a location.
     */
    public Location getLocationWithPrices(Long locationId) {
        return locationRepository.findById(locationId)
            .orElseThrow(() -> new NotFoundException("Location not found with id: " + locationId));
    }
}

