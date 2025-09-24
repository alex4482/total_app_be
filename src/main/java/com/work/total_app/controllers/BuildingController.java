package com.work.total_app.controllers;

import com.work.total_app.constants.AuthenticationConstants;
import com.work.total_app.models.building.Building;
import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.services.BuildingService;
import com.work.total_app.services.RentalSpaceService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/buildings")
@CrossOrigin(origins = {AuthenticationConstants.PROD_WEBSITE_URL, }, originPatterns = {AuthenticationConstants.LOCAL_WEBSITE_PATTERN, AuthenticationConstants.STAGING_WEBSITE_PATTERN})
@Log4j2
public class BuildingController {

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private RentalSpaceService spaceService;

    @GetMapping
    public List<Building> listBuildings()
    {
        return buildingService.listAll();
    }

    @GetMapping({"/spaces", "/{bid}/spaces"})
    public List<RentalSpace> listSpaces(@PathVariable String bid)
    {
        return spaceService.listAll(bid);
    }

    @GetMapping("/{bid}")
    public Building getBuilding(@PathVariable String bid)
    {
        return buildingService.getBuilding(bid);
    }

    @PostMapping
    public Building addBuilding(@RequestBody Building b)
    {
        return buildingService.addBuilding(b);
    }

    @GetMapping({"/spaces/{sid}", "/{bid}/spaces/{sid}"})
    public RentalSpace getSpace(@PathVariable String bid, @PathVariable String sid)
    {
        return spaceService.getSpace(bid, sid);
    }

    @PostMapping({"/spaces", "/{bid}/spaces"})
    public RentalSpace addSpace(@PathVariable String bid, @RequestBody RentalSpace rentalSpace)
    {
        return spaceService.addSpace(bid, rentalSpace);
    }


}
