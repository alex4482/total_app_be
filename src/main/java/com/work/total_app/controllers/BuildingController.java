package com.work.total_app.controllers;

import com.work.total_app.config.LocationExcelColumnConfig;
import com.work.total_app.models.api.ApiResponse;
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
    @Autowired
    private LocationExcelColumnConfig columnConfig;


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

    @GetMapping("/import-columns")
    @ResponseBody
    public ExcelColumnsDto getImportColumns()
    {
        List<String> columns = List.of(
                columnConfig.getLocationType(),
                columnConfig.getName(),
                columnConfig.getOfficialName(),
                columnConfig.getBuildingLocation(),
                columnConfig.getMp(),
                columnConfig.getGroundLevel(),
                columnConfig.getBuildingName()
        );
        return new ExcelColumnsDto(columns);
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
    public ResponseEntity<ApiResponse<Building>> getBuilding(@PathVariable Long bid)
    {
        Building building = buildingService.getBuilding(bid);
        if (building == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Building not found with id: " + bid));
        }
        return ResponseEntity.ok(ApiResponse.success(building));
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ApiResponse<Building>> addBuilding(@RequestBody CreateLocationDto cld)
    {
        Building b = Building.fromDto(cld);
        Building saved = buildingService.addBuilding(b);
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Building created successfully", saved));
    }

    @PatchMapping("/{bid}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Building>> updateBuilding(@PathVariable Long bid, @RequestBody CreateLocationDto dto)
    {
        log.info("Updating building with id: {}", bid);
        Building updated = buildingService.updateBuilding(bid, dto);
        return ResponseEntity.ok(ApiResponse.success("Building updated successfully", updated));
    }

    @DeleteMapping("/{bid}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteBuilding(@PathVariable Long bid)
    {
        log.info("Deleting building with id: {}", bid);
        buildingService.deleteBuilding(bid);
        return ResponseEntity.ok(ApiResponse.success("Building deleted successfully", null));
    }

    @GetMapping({"/spaces/{sid}", "/{bid}/spaces/{sid}"})
    @ResponseBody
    public ResponseEntity<ApiResponse<RentalSpace>> getSpace(@PathVariable Long bid, @PathVariable Long sid)
    {
        RentalSpace space = spaceService.getSpace(bid, sid);
        return ResponseEntity.ok(ApiResponse.success(space));
    }

    @PostMapping({"/spaces", "/{bid}/spaces"})
    @ResponseBody
    public ResponseEntity<ApiResponse<Room>> addSpace(@PathVariable(required = false) Long bid,
                         @RequestBody CreateLocationDto dto)
    {
        Room saved = spaceService.addSpace(bid, dto);
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Space created successfully", saved));
    }

    @PatchMapping("/spaces/{spaceId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Room>> updateSpace(@PathVariable Long spaceId,
                            @RequestBody CreateLocationDto dto)
    {
        log.info("Updating space with id: {}", spaceId);
        Room updated = spaceService.updateSpace(spaceId, dto);
        return ResponseEntity.ok(ApiResponse.success("Space updated successfully", updated));
    }

    @DeleteMapping("/spaces/{spaceId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteSpace(@PathVariable Long spaceId)
    {
        log.info("Deleting space with id: {}", spaceId);
        spaceService.deleteSpace(spaceId);
        return ResponseEntity.ok(ApiResponse.success("Space deleted successfully", null));
    }

    /**
     * Import locations (Building, Room, RentalSpace) from Excel file.
     * Excel format:
     * - Column 0: LocationType (Building, Room, RentalSpace)
     * - Column 1: Name (unique)
     * - Column 2: OfficialName (optional)
     * - Column 3: BuildingLocation (LETCANI, TOMESTI)
     * - Column 4: Mp (square meters)
     * - Column 5: GroundLevel (true/false, only for Room/RentalSpace)
     * - Column 6: BuildingName (parent building name, only for Room/RentalSpace)
     */
    @PostMapping("/import")
    @ResponseBody
    public ResponseEntity<ApiResponse<LocationImportResultDto>> importLocations(
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Importing locations from Excel file: {}",
                    file.getOriginalFilename());
            LocationImportResultDto result = locationImportExportService.importLocationsFromExcel(file);
            return ResponseEntity.ok(ApiResponse.success("Import completed successfully", result));
        } catch (Exception e) {
            log.error("Error importing locations", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to import locations: " + e.getMessage()));
        }
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
