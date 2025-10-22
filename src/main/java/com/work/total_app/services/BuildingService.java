package com.work.total_app.services;

import com.work.total_app.models.building.Building;
import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.building.CreateLocationDto;
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
        if (buildingRepository.existsByName(b.getName()))
        {
            throw new ValidationException("Building with given name already exists: " + b.getName());
        }

        return buildingRepository.save(b);
    }

    public Building getBuilding(Long bid) {
        if (bid == null)
        {
            throw new RuntimeException("No building id given.");
        }

        return buildingRepository.findById(bid).orElse(null);
    }

    public Building updateBuilding(Long bid, CreateLocationDto dto) {
        if (bid == null) {
            throw new ValidationException("No building id provided for update.");
        }

        Building building = buildingRepository.findById(bid)
                .orElseThrow(() -> new ValidationException("Building not found with id: " + bid));

        // Update fields from DTO (only non-null values)
        if (dto.getName() != null) building.setName(dto.getName());
        if (dto.getOfficialName() != null) building.setOfficialName(dto.getOfficialName());
        if (dto.getLocation() != null) building.setLocation(dto.getLocation());
        if (dto.getMp() != null) building.setMp(dto.getMp());
        if (dto.getType() != null) building.setType(dto.getType());
        if (dto.getObservations() != null) building.setObservations(dto.getObservations());

        return buildingRepository.save(building);
    }

    public void deleteBuilding(Long bid) {
        if (bid == null) {
            throw new ValidationException("No building id provided for deletion.");
        }

        Building building = buildingRepository.findById(bid)
                .orElseThrow(() -> new ValidationException("Building not found with id: " + bid));

        // Delete building (rooms will be deleted automatically due to cascade)
        buildingRepository.delete(building);
    }
}
