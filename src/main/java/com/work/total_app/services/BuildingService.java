package com.work.total_app.services;

import com.work.total_app.models.building.Building;
import com.work.total_app.repositories.BuildingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildingService {

    @Autowired
    private BuildingRepository buildingRepository;

    public List<Building> listAll() {
        return buildingRepository.findAll();
    }

    public Building addBuilding(Building b) {
        return buildingRepository.save(b);
    }

    public Building getBuilding(String bid) {
        if (bid == null)
        {
            throw new RuntimeException("No building id given.");
        }

        BuildingIdPair bip = BuildingIdPair.fromString(bid);
        if (bip == null)
        {
            throw new RuntimeException("Given building id pair is not valid. Location invalid.");
        }

        return buildingRepository.findById(bip).orElse(null);
    }
}
