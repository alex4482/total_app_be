package com.work.total_app.controllers;

import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.reading.*;
import com.work.total_app.services.IndexCounterService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/index-counters")
@Log4j2
public class IndexCounterController {

    @Autowired
    private IndexCounterService counterService;

    @GetMapping
    public List<IndexCounter> listCounters(@RequestParam CounterType type,
                                           @RequestParam String locationId,
                                           @RequestParam LocationType locationType,
                                           @RequestParam BuildingLocation buildingLocation)
    {
        return counterService.getCounters(type, locationId, locationType, buildingLocation);
    }

    @GetMapping("/{id}")
    public IndexCounter listCounter(@PathVariable Long id)
    {
        return counterService.getCounter(id);
    }

    @PostMapping
    public IndexCounter addCounter(@RequestBody IndexCounterDto icd)
    {
        return counterService.saveCounter(icd);
    }

    @PostMapping("/data")
    public IndexData addData(@RequestBody IndexDataDto iData)
    {
        return counterService.addData(iData);
    }
}
