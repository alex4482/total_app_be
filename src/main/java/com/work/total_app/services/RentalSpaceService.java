package com.work.total_app.services;

import com.work.total_app.models.building.*;
import com.work.total_app.models.reading.LocationType;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.repositories.BuildingRepository;
import com.work.total_app.repositories.LocationRepository;
import com.work.total_app.repositories.RentalSpaceRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log4j2
public class RentalSpaceService {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private RentalSpaceRepository spaceRepository;

    @Autowired
    private LocationRepository locationRepository;

    public RentalSpace getSpace(Long bid, Long sid) {
        RentalSpace rs = spaceRepository.findById(sid).orElseThrow(
                () -> new NotFoundException("Rental space not found by id: " + sid)
        );
        if (rs == null || rs.getBuilding() == null || rs.getBuilding().getId() == null)
        {
            throw new ValidationException("Data error when getting rental space with bid: " + bid + " and sid: " + sid);
            //TODO: log not present rs?
        }

        if (!rs.getBuilding().getId().equals(bid))
        {
            throw new ValidationException("The space with given id has different building id than the one given in path");
        }

        return rs;
    }

    public Room addSpace(Long bid, CreateLocationDto dto) {
        if (dto == null) {
            throw new RuntimeException("Missing location data object");
        }

        // Determine if we create a Room or RentalSpace based on LocationType
        Room room;
        if (dto.getType() == LocationType.RENTAL_SPACE) {
            room = RentalSpace.fromDto(dto);
        } else {
            room = Room.fromDto(dto);
        }

        if (bid != null) {
            if (room.getBuilding() == null) {
                Building b = buildingRepository.findById(bid).orElseThrow(() ->
                        new NotFoundException("No building found with this id when adding new space."));
                room.setBuilding(b);
            } else {
                if (!room.getBuilding().getId().equals(bid)) {
                    throw new ValidationException("mismatched building ids in rental space obj and in path");
                }
            }

            room.setLocation(room.getBuilding().getLocation());
            room.getBuilding().addRoom(room);
            return locationRepository.save(room);
        }

        if (room.getBuilding() == null || room.getBuilding().getId() == null) {
            throw new ValidationException("no building id for given space");
        }

        room.getBuilding().addRoom(room);
        return locationRepository.save(room);
    }

    public Room updateSpace(Long spaceId, CreateLocationDto dto) {
        // Find the location by ID and determine its type
        Location location = locationRepository.findById(spaceId)
                .orElseThrow(() -> new NotFoundException("Space not found with id: " + spaceId));

        if (!(location instanceof Room room)) {
            throw new ValidationException("Location with id " + spaceId + " is not a Room or RentalSpace");
        }

        // Update fields from DTO (only non-null values)
        if (dto.getName() != null) room.setName(dto.getName());
        if (dto.getOfficialName() != null) room.setOfficialName(dto.getOfficialName());
        if (dto.getLocation() != null) room.setLocation(dto.getLocation());
        if (dto.getMp() != null) room.setMp(dto.getMp());
        if (dto.getType() != null) room.setType(dto.getType());
        if (dto.getObservations() != null) room.setObservations(dto.getObservations());
        if (dto.getGroundLevel() != null) room.setGroundLevel(dto.getGroundLevel());
        if (dto.getBuildingId() != null)
        {
            Building b = buildingRepository.findById(dto.getBuildingId())
                    .orElseThrow(() -> new NotFoundException("Building not found with id: " + dto.getBuildingId()));
            room.setBuilding(b);
            room.setLocation(b.getLocation());
        }

        // Update building if provided
        if (dto.getBuildingId() != null) {
            Building building = buildingRepository.findById(dto.getBuildingId())
                    .orElseThrow(() -> new NotFoundException("Building not found with id: " + dto.getBuildingId()));
            room.setBuilding(building);
        }

        // Save and return (works for both Room and RentalSpace since RentalSpace extends Room)
        return locationRepository.save(room);
    }

    public void deleteSpace(Long spaceId) {
        if (spaceId == null) {
            throw new ValidationException("No space id provided for deletion.");
        }

        Location location = locationRepository.findById(spaceId)
                .orElseThrow(() -> new NotFoundException("Space not found with id: " + spaceId));

        if (!(location instanceof Room)) {
            throw new ValidationException("Location with id " + spaceId + " is not a Room or RentalSpace");
        }

        // Delete space (works for both Room and RentalSpace)
        locationRepository.delete(location);
    }

    public Room updateRoom(Long roomId, CreateLocationDto dto) {
        Room room = locationRepository.findById(roomId)
                .filter(loc -> loc instanceof Room)
                .map(loc -> (Room) loc)
                .orElseThrow(() -> new NotFoundException("Room not found with id: " + roomId));

        // Update fields from DTO
        if (dto.getName() != null) room.setName(dto.getName());
        if (dto.getOfficialName() != null) room.setOfficialName(dto.getOfficialName());
        if (dto.getLocation() != null) room.setLocation(dto.getLocation());
        if (dto.getMp() != null) room.setMp(dto.getMp());
        if (dto.getType() != null) room.setType(dto.getType());
        if (dto.getObservations() != null) room.setObservations(dto.getObservations());
        if (dto.getGroundLevel() != null) room.setGroundLevel(dto.getGroundLevel());

        // Update building if provided
        if (dto.getBuildingId() != null) {
            Building building = buildingRepository.findById(dto.getBuildingId())
                    .orElseThrow(() -> new NotFoundException("Building not found with id: " + dto.getBuildingId()));
            room.setBuilding(building);
            building.addRoom(room);
            locationRepository.save(building);
        }

        return locationRepository.save(room);
    }

    public RentalSpace updateRentalSpace(Long spaceId, CreateLocationDto dto) {
        RentalSpace rentalSpace = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new NotFoundException("RentalSpace not found with id: " + spaceId));

        // Update fields from DTO
        if (dto.getName() != null) rentalSpace.setName(dto.getName());
        if (dto.getOfficialName() != null) rentalSpace.setOfficialName(dto.getOfficialName());
        if (dto.getLocation() != null) rentalSpace.setLocation(dto.getLocation());
        if (dto.getMp() != null) rentalSpace.setMp(dto.getMp());
        if (dto.getType() != null) rentalSpace.setType(dto.getType());
        if (dto.getObservations() != null) rentalSpace.setObservations(dto.getObservations());
        if (dto.getGroundLevel() != null) rentalSpace.setGroundLevel(dto.getGroundLevel());

        // Update building if provided
        if (dto.getBuildingId() != null) {
            Building building = buildingRepository.findById(dto.getBuildingId())
                    .orElseThrow(() -> new NotFoundException("Building not found with id: " + dto.getBuildingId()));
            rentalSpace.setBuilding(building);
        }

        return spaceRepository.save(rentalSpace);
    }

    public List<RentalSpace> listAll(BuildingLocation buildingLocation, Boolean groundLevel, Boolean empty) {
        RentalSpaceFilter f = new RentalSpaceFilter(null, buildingLocation, groundLevel, empty, null);
        return spaceRepository.findAll(RentalSpace.byFilter(f));
    }

    public List<RentalSpace> listAllFromBuilding(String bid, Boolean groundLevel, Boolean empty) {
        if (bid == null)
        {
            throw new ValidationException("Missing building id for listing all spaces in building.");
        }

        RentalSpaceFilter f = new RentalSpaceFilter(bid, null, groundLevel, empty, null);
        return spaceRepository.findAll(RentalSpace.byFilter(f));
    }

    public List<RentalSpace> listAllByTenant(String buildingId,
                                             String tenantId,
                                             BuildingLocation buildingLocation,
                                             Boolean groundLevel) {
        if (tenantId == null)
        {
            throw new ValidationException("Missing tenant id for listing all spaces rented by tenant.");
        }
        RentalSpaceFilter f = new RentalSpaceFilter(null, buildingLocation, groundLevel, false, tenantId);
        return spaceRepository.findAll(RentalSpace.byFilter(f));
    }
}
