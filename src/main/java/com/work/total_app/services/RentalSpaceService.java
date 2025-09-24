package com.work.total_app.services;

import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.repositories.BuildingRepository;
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

    public List<RentalSpace> listAll(String bid) {
        if (bid == null)
        {
            return spaceRepository.findAll();
        }
        BuildingIdPair bip = BuildingIdPair.fromString(bid);
        if (bip == null)
        {
            throw new RuntimeException("Given building id pair is not valid. Location invalid.");
        }

        return spaceRepository.findByBuildingId(bip);
    }

    public RentalSpace getSpace(String bid, String sid) {
        RentalSpace rs = spaceRepository.findById(sid).orElse(null);
        if (rs == null)
        {
            //TODO: log not present rs?
            return null;
        }

        if (!rs.getBuildingId().getName().equals(bid))
        {
            throw new RuntimeException("The space with given id has different building id than the one given in path");
            //TODO: wtf, log? error?
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
            BuildingIdPair buildingId = BuildingIdPair.fromString(bid);
            if (buildingId == null)
            {
                throw new RuntimeException("given building id is not valid id. no location found.");
            }

            if (rentalSpace.getBuildingId() == null)
            {
                rentalSpace.setBuildingId(buildingId);
            }
            else {
                if (!rentalSpace.getBuildingId().equals(buildingId))
                {
                    throw new RuntimeException("mismatched building ids in rental space obj and in path");
                }
            }

            return spaceRepository.save(rentalSpace);
        }

        if (rentalSpace.getBuildingId() == null)
        {
            throw new RuntimeException("no building id for given space");
        }

        return spaceRepository.save(rentalSpace);
    }
}
