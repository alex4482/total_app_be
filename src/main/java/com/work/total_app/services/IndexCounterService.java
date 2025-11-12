package com.work.total_app.services;

import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.building.Location;
import com.work.total_app.models.reading.*;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.repositories.IndexCounterRepository;
import com.work.total_app.repositories.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IndexCounterService {

    @Autowired
    private IndexCounterRepository counterRepository;

    @Autowired
    private LocationRepository locationRepository;

    public List<IndexCounter> getCounters(CounterType type,
                                          String locationId,
                                          LocationType locationType,
                                          BuildingLocation buildingLocation) {
        IndexCounterFilter icf = new IndexCounterFilter(type, locationId, locationType, buildingLocation);
        return counterRepository.findAll(IndexCounter.byFilter(icf));
    }

    public IndexCounter getCounter(Long id) {
        return counterRepository.findById(id).orElseThrow();
    }

    public IndexCounter saveCounter(IndexCounterDto icd) {
        Location l = locationRepository.findById(icd.getLocationId()).orElse(null);
        if (l == null)
        {
            throw new NotFoundException("No location with this id exists: " + icd.getLocationId());
        }
        IndexCounter ic = IndexCounter.fromDto(icd, l);
        l.addCounter(ic);
        return counterRepository.save(ic);
    }

    @Transactional
    public IndexCounter updateCounter(Long counterId, IndexCounterDto dto) {
        // Find existing counter
        IndexCounter counter = counterRepository.findById(counterId)
            .orElseThrow(() -> new NotFoundException("Counter not found with id: " + counterId));
        
        // Update basic fields
        if (dto.getName() != null) {
            counter.setName(dto.getName());
        }
        if (dto.getCounterType() != null) {
            counter.setCounterType(dto.getCounterType());
        }
        if (dto.getLocationType() != null) {
            counter.setLocationType(dto.getLocationType());
        }
        if (dto.getBuildingLocation() != null) {
            counter.setBuildingLocation(dto.getBuildingLocation());
        }
        if (dto.getDefaultUnitPrice() != null) {
            counter.setDefaultUnitPrice(dto.getDefaultUnitPrice());
        }
        
        // Update location if changed
        if (dto.getLocationId() != null && !dto.getLocationId().equals(counter.getLocation().getId())) {
            Location oldLocation = counter.getLocation();
            Location newLocation = locationRepository.findById(dto.getLocationId())
                .orElseThrow(() -> new NotFoundException("Location not found with id: " + dto.getLocationId()));
            
            // Remove from old location
            oldLocation.removeCounter(counter);
            
            // Add to new location
            newLocation.addCounter(counter);
            counter.setLocation(newLocation);
        }
        
        return counterRepository.save(counter);
    }

    public IndexData addData(IndexDataDto iData) {
        IndexData data = new IndexData();
        IndexCounter ic = counterRepository.findById(iData.getCounterId()).orElseThrow(
                () -> new NotFoundException("Cant find counter for adding new index data, with id: " + iData.getCounterId())
        );

        data.setIndex(iData.getIndex());
        data.setReadingDate(iData.getReadingDate());
        data.setUnitPrice(iData.getUnitPrice()); // Can be null - will use counter's defaultUnitPrice
        
        ic.addIndexData(data);
        
        return counterRepository.save(ic).getIndexData().get(0); // Return the newly added data
    }

    /**
     * Update the default unit price for a counter (global price).
     * Optionally recalculates all costs for readings that don't have local prices.
     */
    public IndexCounter updateDefaultUnitPrice(Long counterId, Double newPrice, boolean recalculateAll) {
        IndexCounter counter = counterRepository.findById(counterId)
            .orElseThrow(() -> new NotFoundException("Counter not found with id: " + counterId));
        
        counter.setDefaultUnitPrice(newPrice);
        
        if (recalculateAll) {
            // Recalculate costs for all readings that use the default price
            for (IndexData data : counter.getIndexData()) {
                if (data.getUnitPrice() == null && data.getConsumption() != null) {
                    // This reading uses the default price, so recalculate
                    data.setTotalCost(data.getConsumption() * newPrice);
                }
            }
        }
        
        return counterRepository.save(counter);
    }

    /**
     * Update the local unit price for a specific reading.
     * Recalculates the total cost for that reading.
     */
    public IndexData updateReadingUnitPrice(Long readingId, Double newPrice) {
        // Note: We need an IndexDataRepository for this, or fetch through counter
        // For now, let's implement through counter
        IndexCounter counter = counterRepository.findAll().stream()
            .filter(c -> c.getIndexData().stream().anyMatch(d -> d.getId().equals(readingId)))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Reading not found with id: " + readingId));
        
        IndexData reading = counter.getIndexData().stream()
            .filter(d -> d.getId().equals(readingId))
            .findFirst()
            .orElseThrow();
        
        reading.setUnitPrice(newPrice);
        
        // Recalculate total cost
        if (reading.getConsumption() != null) {
            Double effectivePrice = reading.getEffectiveUnitPrice();
            if (effectivePrice != null) {
                reading.setTotalCost(reading.getConsumption() * effectivePrice);
            }
        }
        
        counterRepository.save(counter);
        return reading;
    }

    /**
     * Replaces an old counter with a new one.
     * 1. Adds a final reading to the old counter
     * 2. Creates a new counter with the same characteristics
     * 3. Adds a ReplacedCounterIndexData to the new counter that references the old counter's final reading
     * 
     * @param dto Contains old counter ID, final index, new counter initial index, and replacement date
     * @return The newly created counter
     */
    @Transactional
    public IndexCounter replaceCounter(ReplaceCounterDto dto) {
        // 1. Find the old counter
        IndexCounter oldCounter = counterRepository.findById(dto.getOldCounterId())
            .orElseThrow(() -> new NotFoundException("Old counter not found with id: " + dto.getOldCounterId()));
        
        // 2. Add final reading to old counter
        IndexData finalReading = new IndexData();
        finalReading.setIndex(dto.getOldCounterFinalIndex());
        finalReading.setReadingDate(dto.getReplacementDate());
        finalReading.setUnitPrice(null); // Will use counter's default or location's default
        oldCounter.addIndexData(finalReading);
        counterRepository.save(oldCounter);
        
        // 3. Create new counter with same characteristics as old counter
        IndexCounter newCounter = new IndexCounter();
        newCounter.setName(dto.getNewCounterName());
        newCounter.setCounterType(dto.getCounterType() != null ? dto.getCounterType() : oldCounter.getCounterType());
        newCounter.setLocationType(dto.getLocationType() != null ? dto.getLocationType() : oldCounter.getLocationType());
        newCounter.setBuildingLocation(dto.getBuildingLocation() != null ? dto.getBuildingLocation() : oldCounter.getBuildingLocation());
        newCounter.setLocation(oldCounter.getLocation());
        newCounter.setDefaultUnitPrice(dto.getDefaultUnitPrice() != null ? dto.getDefaultUnitPrice() : oldCounter.getDefaultUnitPrice());
        
        // 4. Create ReplacedCounterIndexData for the new counter
        ReplacedCounterIndexData replacementData = new ReplacedCounterIndexData();
        replacementData.setIndex(dto.getNewCounterInitialIndex());
        replacementData.setReadingDate(dto.getReplacementDate());
        replacementData.setOldIndexData(finalReading);
        replacementData.setNewCounterInitialIndex(dto.getNewCounterInitialIndex());
        replacementData.setReplacementDate(dto.getReplacementDate());
        replacementData.setUnitPrice(null); // Will use counter's default
        
        // Add the replacement data to the new counter
        newCounter.addIndexData(replacementData);
        
        // 5. Add new counter to location and save
        oldCounter.getLocation().addCounter(newCounter);
        IndexCounter savedCounter = counterRepository.save(newCounter);
        
        return savedCounter;
    }
}
