package com.work.total_app.services;

import com.work.total_app.models.building.Building;
import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.repositories.BuildingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildingService {

    @Autowired
    private BuildingRepository buildingRepository;

    public List<Building> listAll(BuildingLocation buildingLocation) {
        return buildingRepository.findAll();
    }

    public Building addBuilding(Building b) {
        if (b.getLocation() == null || b.getName() == null)
        {
            throw new ValidationException("No location/name for given building to add.");
        }
        b.setId(b.getLocation() + "-" + b.getName());
        return buildingRepository.save(b);
    }

    public Building getBuilding(String bid) {
        if (bid == null)
        {
            throw new RuntimeException("No building id given.");
        }

        return buildingRepository.findById(bid).orElse(null);
    }
}
