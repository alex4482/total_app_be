package com.work.total_app.services;

import com.work.total_app.models.building.Building;
import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.building.RentalSpaceFilter;
import com.work.total_app.models.reading.IndexCounter;
import com.work.total_app.models.reading.IndexCounterFilter;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.repositories.BuildingRepository;
import com.work.total_app.repositories.RentalSpaceRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
@Log4j2
public class RentalSpaceService {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private RentalSpaceRepository spaceRepository;

    public RentalSpace getSpace(String bid, String sid) {
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

    public RentalSpace addSpace(String bid, RentalSpace rentalSpace) {

        if (rentalSpace == null)
        {
            throw new RuntimeException("Missing rental space object");
        }

        if (bid != null)
        {
            if (rentalSpace.getBuilding() == null)
            {
                Building b = buildingRepository.findById(bid).orElseThrow(() ->
                        new NotFoundException("No building found with this id when creating new rental space."));
                rentalSpace.setBuilding(b);
            }
            else {
                if (!rentalSpace.getBuilding().getId().equals(bid))
                {
                    throw new ValidationException("mismatched building ids in rental space obj and in path");
                }
            }

            return spaceRepository.save(rentalSpace);
        }

        if (rentalSpace.getBuilding() == null || rentalSpace.getBuilding().getId() == null)
        {
            throw new ValidationException("no building id for given space");
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
