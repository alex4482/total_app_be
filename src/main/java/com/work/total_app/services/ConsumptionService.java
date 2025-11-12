package com.work.total_app.services;

import com.work.total_app.models.reading.IndexCounter;
import com.work.total_app.models.reading.IndexData;
import com.work.total_app.repositories.IndexCounterRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Log4j2
public class ConsumptionService {

    @Autowired
    private IndexCounterRepository counterRepository;

    /**
     * Calculate consumption between two readings for a specific counter.
     * Returns the difference in index values.
     */
    public Double calculateConsumptionBetweenReadings(Long counterId, Date fromDate, Date toDate) {
        IndexCounter counter = counterRepository.findById(counterId).orElseThrow();
        
        List<IndexData> readings = counter.getIndexData().stream()
            .filter(r -> !r.getReadingDate().before(fromDate) && !r.getReadingDate().after(toDate))
            .sorted(Comparator.comparing(IndexData::getReadingDate))
            .toList();
            
        if (readings.size() < 2) {
            return 0.0;
        }
        
        IndexData firstReading = readings.get(0);
        IndexData lastReading = readings.get(readings.size() - 1);
        
        return lastReading.getIndex() - firstReading.getIndex();
    }

    /**
     * Calculate total cost between two dates for a specific counter.
     */
    public Double calculateCostBetweenDates(Long counterId, Date fromDate, Date toDate) {
        IndexCounter counter = counterRepository.findById(counterId).orElseThrow();
        
        return counter.getIndexData().stream()
            .filter(r -> r.getReadingDate().after(fromDate) && !r.getReadingDate().after(toDate))
            .filter(r -> r.getTotalCost() != null)
            .mapToDouble(IndexData::getTotalCost)
            .sum();
    }

    /**
     * Get consumption summary for a location between two dates.
     * Returns a map of CounterType -> total consumption.
     */
    public Map<String, Double> getConsumptionSummaryForLocation(String locationId, Date fromDate, Date toDate) {
        // This will be implemented to get all counters for a location
        // and calculate total consumption per type
        return new HashMap<>();
    }
}

