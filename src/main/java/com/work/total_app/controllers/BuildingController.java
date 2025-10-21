package com.work.total_app.controllers;

import com.work.total_app.models.building.Building;
import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.building.Room;
import com.work.total_app.models.reading.LocationType;
import com.work.total_app.services.BuildingService;
import com.work.total_app.services.RentalSpaceService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/buildings")
@Log4j2
public class BuildingController {

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private RentalSpaceService spaceService;

    @GetMapping("/locations")
    @ResponseBody
    public List<String> listBuildingLocations()
    {
        return java.util.Arrays.stream(BuildingLocation.values())
                .map(Enum::name)
                .toList();
    }

    @GetMapping("/location-types")
    @ResponseBody
    public List<String> listLocationTypes()
    {
        return java.util.Arrays.stream(LocationType.values())
                .map(Enum::name)
                .toList();
    }

    @GetMapping
    @ResponseBody
    public List<Building> listBuildings(@RequestParam BuildingLocation buildingLocation)
    {
        return buildingService.listAll(buildingLocation);
    }

    @GetMapping("/{bid}/spaces")
    public List<RentalSpace> listSpacesFromBuilding(@PathVariable String bid,
                                                    @RequestParam Boolean groundLevel,
                                                    @RequestParam Boolean empty)
    {
        return spaceService.listAllFromBuilding(bid, groundLevel, empty);
    }

    @GetMapping("/spaces")
    public List<RentalSpace> listSpaces(@RequestParam BuildingLocation buildingLocation,
                                        @RequestParam Boolean groundLevel,
                                        @RequestParam Boolean empty)
    {
        return spaceService.listAll(buildingLocation, groundLevel, empty);
    }

    @GetMapping("{bid}/spaces/{tid}")
    public List<RentalSpace> listSpacesInBuildingRentedByTenant(
            @PathVariable String buildingId,
            @PathVariable String tenantId,
            @RequestParam BuildingLocation buildingLocation,
            @RequestParam Boolean groundLevel)
    {
        return spaceService.listAllByTenant(buildingId, tenantId, buildingLocation, groundLevel);
    }

    @GetMapping("/spaces/{tid}")
    public List<RentalSpace> listSpacesRentedByTenant(@PathVariable String tenantId,
                                        @RequestParam BuildingLocation buildingLocation,
                                        @RequestParam Boolean groundLevel)
    {
        return spaceService.listAllByTenant(null, tenantId, buildingLocation, groundLevel);
    }

    @GetMapping("/{bid}")
    public Building getBuilding(@PathVariable Long bid)
    {
        return buildingService.getBuilding(bid);
    }

    @PostMapping
    public Building addBuilding(@RequestBody Building b)
    {
        return buildingService.addBuilding(b);
    }

    @GetMapping({"/spaces/{sid}", "/{bid}/spaces/{sid}"})
    public RentalSpace getSpace(@PathVariable Long bid, @PathVariable Long sid)
    {
        return spaceService.getSpace(bid, sid);
    }

    @PostMapping({"/spaces", "/{bid}/spaces"})
    public Room addSpace(@PathVariable Long bid, @RequestBody Room newSpace)
    {
        return spaceService.addSpace(bid, newSpace);
    }
}
