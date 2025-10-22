package com.work.total_app.controllers;

import com.work.total_app.models.building.*;
import com.work.total_app.models.reading.LocationType;
import com.work.total_app.services.BuildingService;
import com.work.total_app.services.LocationImportExportService;
import com.work.total_app.services.RentalSpaceService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/buildings")
@Log4j2
public class BuildingController {

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private RentalSpaceService spaceService;

    @Autowired
    private LocationImportExportService locationImportExportService;

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
    public List<Building> listBuildings(@RequestParam(required = false) BuildingLocation buildingLocation)
    {
        return buildingService.listAll(buildingLocation);
    }

    @GetMapping("/{bid}/spaces")
    @ResponseBody
    public List<RentalSpace> listSpacesFromBuilding(@PathVariable String bid,
                                                    @RequestParam(required = false) Boolean groundLevel,
                                                    @RequestParam(required = false) Boolean empty)
    {
        return spaceService.listAllFromBuilding(bid, groundLevel, empty);
    }

    @GetMapping("/spaces")
    @ResponseBody
    public List<RentalSpace> listSpaces(@RequestParam(required = false) BuildingLocation buildingLocation,
                                        @RequestParam(required = false) Boolean groundLevel,
                                        @RequestParam(required = false) Boolean empty)
    {
        return spaceService.listAll(buildingLocation, groundLevel, empty);
    }

    @GetMapping("{bid}/spaces/{tid}")
    @ResponseBody
    public List<RentalSpace> listSpacesInBuildingRentedByTenant(
            @PathVariable String buildingId,
            @PathVariable String tenantId,
            @RequestParam(required = false) BuildingLocation buildingLocation,
            @RequestParam(required = false) Boolean groundLevel)
    {
        return spaceService.listAllByTenant(buildingId, tenantId, buildingLocation, groundLevel);
    }

    @GetMapping("/spaces/{tid}")
    @ResponseBody
    public List<RentalSpace> listSpacesRentedByTenant(@PathVariable String tenantId,
                                        @RequestParam(required = false) BuildingLocation buildingLocation,
                                        @RequestParam(required = false) Boolean groundLevel)
    {
        return spaceService.listAllByTenant(null, tenantId, buildingLocation, groundLevel);
    }

    @GetMapping("/{bid}")
    @ResponseBody
    public Building getBuilding(@PathVariable Long bid)
    {
        return buildingService.getBuilding(bid);
    }

    @PostMapping
    @ResponseBody
    public Building addBuilding(@RequestBody CreateLocationDto cld)
    {
        Building b = Building.fromDto(cld);
        return buildingService.addBuilding(b);
    }

    @PatchMapping("/{bid}")
    @ResponseBody
    public Building updateBuilding(@PathVariable Long bid, @RequestBody CreateLocationDto dto)
    {
        log.info("Updating building with id: {}", bid);
        return buildingService.updateBuilding(bid, dto);
    }

    @DeleteMapping("/{bid}")
    @ResponseBody
    public void deleteBuilding(@PathVariable Long bid)
    {
        log.info("Deleting building with id: {}", bid);
        buildingService.deleteBuilding(bid);
    }

    @GetMapping({"/spaces/{sid}", "/{bid}/spaces/{sid}"})
    @ResponseBody
    public RentalSpace getSpace(@PathVariable Long bid, @PathVariable Long sid)
    {
        return spaceService.getSpace(bid, sid);
    }

    @PostMapping({"/spaces", "/{bid}/spaces"})
    @ResponseBody
    public Room addSpace(@PathVariable(required = false) Long bid,
                         @RequestBody CreateLocationDto dto)
    {
        return spaceService.addSpace(bid, dto);
    }

    @PatchMapping("/spaces/{spaceId}")
    @ResponseBody
    public Room updateSpace(@PathVariable Long spaceId,
                            @RequestBody CreateLocationDto dto)
    {
        log.info("Updating space with id: {}", spaceId);
        return spaceService.updateSpace(spaceId, dto);
    }

    @DeleteMapping("/spaces/{spaceId}")
    @ResponseBody
    public void deleteSpace(@PathVariable Long spaceId)
    {
        log.info("Deleting space with id: {}", spaceId);
        spaceService.deleteSpace(spaceId);
    }

    /**
     * Import locations (Building, Room, RentalSpace) from Excel file.
     * Excel format:
     * - Column 0: Type (Building, Room, RentalSpace)
     * - Column 1: Name
     * - Column 2: OfficialName (optional)
     * - Column 3: BuildingLocation (e.g., PRAHOVA, ALBA)
     * - Column 4: Mp (square meters)
     * - Column 5: LocationType (e.g., OFFICE, WAREHOUSE)
     * - Column 6: GroundLevel (true/false, only for Room/RentalSpace)
     * - Column 7: BuildingId (parent building ID, only for Room/RentalSpace)
     */
    @PostMapping("/import")
    @ResponseBody
    public LocationImportResultDto importLocations(
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Importing locations from Excel file: {}",
                file.getOriginalFilename());
        return locationImportExportService.importLocationsFromExcel(file);
    }

    /**
     * Export all locations (Building, Room, RentalSpace) to Excel file.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLocations() throws IOException {
        log.info("Exporting all locations to Excel");
        byte[] excelData = locationImportExportService.exportLocationsToExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "locations-export.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}
