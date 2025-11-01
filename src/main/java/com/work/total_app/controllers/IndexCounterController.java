package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.reading.*;
import com.work.total_app.services.IndexCounterService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<IndexCounter>> listCounter(@PathVariable Long id)
    {
        IndexCounter counter = counterService.getCounter(id);
        return ResponseEntity.ok(ApiResponse.success(counter));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IndexCounter>> addCounter(@RequestBody IndexCounterDto icd)
    {
        IndexCounter saved = counterService.saveCounter(icd);
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Counter created successfully", saved));
    }

    @PostMapping("/data")
    public ResponseEntity<ApiResponse<IndexData>> addData(@RequestBody IndexDataDto iData)
    {
        IndexData data = counterService.addData(iData);
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Index data added successfully", data));
    }
}
