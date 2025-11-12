package com.work.total_app.services;

import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.reading.*;
import com.work.total_app.repositories.IndexCounterRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ConsumptionStatisticsService {

    @Autowired
    private IndexCounterRepository counterRepository;

    /**
     * Get consumption statistics for a specific period.
     * @param startDate Start of period
     * @param endDate End of period
     * @param buildingLocation Optional filter by building location
     * @return Aggregated statistics
     */
    public ConsumptionStatistics getStatistics(Date startDate, Date endDate, BuildingLocation buildingLocation) {
        List<IndexCounter> counters;
        
        if (buildingLocation != null) {
            counters = counterRepository.findAll().stream()
                .filter(c -> c.getBuildingLocation() == buildingLocation)
                .toList();
        } else {
            counters = counterRepository.findAll();
        }
        
        ConsumptionStatistics stats = new ConsumptionStatistics();
        stats.setPeriod(formatPeriod(startDate, endDate));
        
        // Statistics by counter type
        Map<String, ConsumptionStatistics.CounterTypeStats> typeStats = new HashMap<>();
        for (CounterType type : CounterType.values()) {
            List<IndexCounter> typeCounters = counters.stream()
                .filter(c -> c.getCounterType() == type)
                .toList();
            
            double totalConsumption = 0;
            double totalCost = 0;
            int readingsCount = 0;
            
            for (IndexCounter counter : typeCounters) {
                List<IndexData> periodReadings = counter.getIndexData().stream()
                    .filter(r -> !r.getReadingDate().before(startDate) && !r.getReadingDate().after(endDate))
                    .toList();
                
                totalConsumption += periodReadings.stream()
                    .filter(r -> r.getConsumption() != null)
                    .mapToDouble(IndexData::getConsumption)
                    .sum();
                
                totalCost += periodReadings.stream()
                    .filter(r -> r.getTotalCost() != null)
                    .mapToDouble(IndexData::getTotalCost)
                    .sum();
                
                readingsCount += periodReadings.size();
            }
            
            typeStats.put(type.name(), new ConsumptionStatistics.CounterTypeStats(
                type.name(), totalConsumption, totalCost, readingsCount
            ));
        }
        stats.setByCounterType(typeStats);
        
        // Statistics by location
        Map<String, ConsumptionStatistics.LocationStats> locationStats = counters.stream()
            .collect(Collectors.groupingBy(
                c -> c.getLocation().getName(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    locationCounters -> calculateLocationStats(locationCounters, startDate, endDate)
                )
            ));
        stats.setByLocation(locationStats);
        
        // Statistics by building
        Map<String, ConsumptionStatistics.BuildingStats> buildingStats = counters.stream()
            .filter(c -> c.getLocation().getClass().getSimpleName().equals("RentalSpace") ||
                        c.getLocation().getClass().getSimpleName().equals("Room"))
            .collect(Collectors.groupingBy(
                c -> {
                    try {
                        var building = c.getLocation().getClass().getMethod("getBuilding").invoke(c.getLocation());
                        return building != null ? building.toString() : "Unknown";
                    } catch (Exception e) {
                        return "Unknown";
                    }
                },
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    buildingCounters -> calculateBuildingStats(buildingCounters, startDate, endDate)
                )
            ));
        stats.setByBuilding(buildingStats);
        
        // Overall totals
        stats.setTotalConsumption(typeStats.values().stream()
            .mapToDouble(ConsumptionStatistics.CounterTypeStats::getTotalConsumption)
            .sum());
        stats.setTotalCost(typeStats.values().stream()
            .mapToDouble(ConsumptionStatistics.CounterTypeStats::getTotalCost)
            .sum());
        
        return stats;
    }

    /**
     * Get statistics by counter type for a period.
     */
    public Map<String, ConsumptionStatistics.CounterTypeStats> getStatsByCounterType(
            Date startDate, Date endDate, CounterType counterType) {
        
        List<IndexCounter> counters = counterRepository.findAll().stream()
            .filter(c -> counterType == null || c.getCounterType() == counterType)
            .toList();
        
        Map<String, ConsumptionStatistics.CounterTypeStats> stats = new HashMap<>();
        
        for (IndexCounter counter : counters) {
            List<IndexData> periodReadings = counter.getIndexData().stream()
                .filter(r -> !r.getReadingDate().before(startDate) && !r.getReadingDate().after(endDate))
                .toList();
            
            double totalConsumption = periodReadings.stream()
                .filter(r -> r.getConsumption() != null)
                .mapToDouble(IndexData::getConsumption)
                .sum();
            
            double totalCost = periodReadings.stream()
                .filter(r -> r.getTotalCost() != null)
                .mapToDouble(IndexData::getTotalCost)
                .sum();
            
            stats.put(counter.getName(), new ConsumptionStatistics.CounterTypeStats(
                counter.getCounterType().name(), totalConsumption, totalCost, periodReadings.size()
            ));
        }
        
        return stats;
    }

    private ConsumptionStatistics.LocationStats calculateLocationStats(
            List<IndexCounter> counters, Date startDate, Date endDate) {
        
        if (counters.isEmpty()) {
            return new ConsumptionStatistics.LocationStats("", "", 0.0, 0.0, new HashMap<>());
        }
        
        String locationName = counters.get(0).getLocation().getName();
        String locationId = counters.get(0).getLocation().getName();
        
        double totalConsumption = 0;
        double totalCost = 0;
        Map<String, Double> byType = new HashMap<>();
        
        for (IndexCounter counter : counters) {
            List<IndexData> periodReadings = counter.getIndexData().stream()
                .filter(r -> !r.getReadingDate().before(startDate) && !r.getReadingDate().after(endDate))
                .toList();
            
            double counterConsumption = periodReadings.stream()
                .filter(r -> r.getConsumption() != null)
                .mapToDouble(IndexData::getConsumption)
                .sum();
            
            double counterCost = periodReadings.stream()
                .filter(r -> r.getTotalCost() != null)
                .mapToDouble(IndexData::getTotalCost)
                .sum();
            
            totalConsumption += counterConsumption;
            totalCost += counterCost;
            
            byType.merge(counter.getCounterType().name(), counterConsumption, Double::sum);
        }
        
        return new ConsumptionStatistics.LocationStats(
            locationId, locationName, totalConsumption, totalCost, byType
        );
    }

    private ConsumptionStatistics.BuildingStats calculateBuildingStats(
            List<IndexCounter> counters, Date startDate, Date endDate) {
        
        if (counters.isEmpty()) {
            return new ConsumptionStatistics.BuildingStats(0L, "", 0.0, 0.0, new HashMap<>());
        }
        
        double totalConsumption = 0;
        double totalCost = 0;
        Map<String, Double> byType = new HashMap<>();
        
        for (IndexCounter counter : counters) {
            List<IndexData> periodReadings = counter.getIndexData().stream()
                .filter(r -> !r.getReadingDate().before(startDate) && !r.getReadingDate().after(endDate))
                .toList();
            
            double counterConsumption = periodReadings.stream()
                .filter(r -> r.getConsumption() != null)
                .mapToDouble(IndexData::getConsumption)
                .sum();
            
            double counterCost = periodReadings.stream()
                .filter(r -> r.getTotalCost() != null)
                .mapToDouble(IndexData::getTotalCost)
                .sum();
            
            totalConsumption += counterConsumption;
            totalCost += counterCost;
            
            byType.merge(counter.getCounterType().name(), counterConsumption, Double::sum);
        }
        
        return new ConsumptionStatistics.BuildingStats(
            0L, "Building", totalConsumption, totalCost, byType
        );
    }

    private String formatPeriod(Date startDate, Date endDate) {
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);
        
        if (start.get(Calendar.YEAR) == end.get(Calendar.YEAR)) {
            if (start.get(Calendar.MONTH) == end.get(Calendar.MONTH)) {
                return String.format("%d-%02d", start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1);
            }
            return String.valueOf(start.get(Calendar.YEAR));
        }
        
        return start.get(Calendar.YEAR) + "-" + end.get(Calendar.YEAR);
    }
}

