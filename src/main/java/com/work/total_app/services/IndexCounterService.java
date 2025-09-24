package com.work.total_app.services;

import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.reading.CounterType;
import com.work.total_app.models.reading.IndexCounter;
import com.work.total_app.models.reading.IndexCounterFilter;
import com.work.total_app.models.reading.IndexCounterLocationType;
import com.work.total_app.repositories.IndexCounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexCounterService {

    @Autowired
    private IndexCounterRepository counterRepository;

    public List<IndexCounter> getCounters(CounterType type,
                                          String locationId,
                                          IndexCounterLocationType locationType,
                                          BuildingLocation buildingLocation) {
        IndexCounterFilter icf = new IndexCounterFilter(type, locationId, locationType, buildingLocation);
        return counterRepository.findAll(IndexCounter.byFilter(icf));
    }

    public IndexCounter getCounter(Long id) {
        return counterRepository.findById(id).orElseThrow();
    }
}
