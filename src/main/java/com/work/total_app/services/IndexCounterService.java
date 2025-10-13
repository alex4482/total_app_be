package com.work.total_app.services;

import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.building.Location;
import com.work.total_app.models.reading.*;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.repositories.IndexCounterRepository;
import com.work.total_app.repositories.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public IndexData addData(IndexDataDto iData) {
        IndexData data = new IndexData();
        IndexCounter ic = counterRepository.findById(iData.getCounterId()).orElseThrow(
                () -> new NotFoundException("Cant find counter for adding new index data, with id: " + iData.getCounterId())
        );

        data.setIndex(iData.getIndex());
        data.setReadingDate(iData.getReadingDate());
        ic.addIndexData(data);
        return data;
    }
}
