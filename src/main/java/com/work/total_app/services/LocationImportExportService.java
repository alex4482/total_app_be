package com.work.total_app.services;

import com.work.total_app.config.LocationExcelColumnConfig;
import com.work.total_app.models.building.*;
import com.work.total_app.models.reading.LocationType;
import com.work.total_app.repositories.BuildingRepository;
import com.work.total_app.repositories.LocationRepository;
import com.work.total_app.repositories.RentalSpaceRepository;
import com.work.total_app.repositories.RoomRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
@Log4j2
public class LocationImportExportService {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RentalSpaceRepository rentalSpaceRepository;

    @Autowired
    private LocationExcelColumnConfig columnConfig;

    /**
     * Import locations from Excel file.
     * Excel format:
     * - Column 0: LocationType (Building, Room, RentalSpace)
     * - Column 1: Name (unique)
     * - Column 2: OfficialName (optional)
     * - Column 3: BuildingLocation (Letcani, Tomesti)
     * - Column 4: Mp (square meters)
     * - Column 5: GroundLevel (true/false, only for Room/RentalSpace)
     * - Column 6: BuildingName (parent building name, only for Room/RentalSpace)
     */
    public LocationImportResultDto importLocationsFromExcel(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int totalRows = 0;

        // Save to temporary file
        java.io.File tempFile = java.io.File.createTempFile("location-import-", ".xlsx");
        try {
            file.transferTo(tempFile);

            try (Workbook workbook = WorkbookFactory.create(tempFile)) {
                Sheet sheet = workbook.getSheetAt(0);

                // Read header row
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    throw new IllegalArgumentException("Excel file must have a header row");
                }

                Map<String, Integer> columnMap = buildColumnMap(headerRow);

                // Process data rows
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    // Get LocationType and name using configured column names
                    String locationTypeStr = getCellValueAsString(row.getCell(columnMap.getOrDefault(normalizeColumnName(columnConfig.getLocationType()), 0)));
                    String name = getCellValueAsString(row.getCell(columnMap.getOrDefault(normalizeColumnName(columnConfig.getName()), 1)));

                    if (locationTypeStr == null || locationTypeStr.trim().isEmpty() || name == null || name.trim().isEmpty()) {
                        continue; // Skip empty rows
                    }

                    totalRows++;

                    try {
                        locationTypeStr = locationTypeStr.trim().toUpperCase().replace(" ", "_");
                        name = name.trim();

                        // Parse LocationType
                        LocationType locationType;
                        try {
                            locationType = LocationType.valueOf(locationTypeStr);
                        } catch (IllegalArgumentException e) {
                            errors.add("Row " + (i + 1) + ": Invalid LocationType '" + locationTypeStr + "'. Valid values: BUILDING, ROOM, RENTAL_SPACE");
                            skipped++;
                            continue;
                        }

                        Location location = null;
                        boolean isUpdate = false;

                        // Check if location exists by name (unique constraint)
                        if (locationType == LocationType.BUILDING) {
                            location = findBuildingByName(name);
                            if (location != null) {
                                isUpdate = true;
                            } else {
                                location = new Building();
                            }
                        } else if (locationType == LocationType.ROOM) {
                            location = findRoomByName(name);
                            if (location != null) {
                                isUpdate = true;
                            } else {
                                location = new Room();
                            }
                        } else if (locationType == LocationType.RENTAL_SPACE) {
                            location = findRentalSpaceByName(name);
                            if (location != null) {
                                isUpdate = true;
                            } else {
                                location = new RentalSpace();
                            }
                        }

                        if (location == null) {
                            errors.add("Row " + (i + 1) + ": Location with name '" + name + "' not found");
                            skipped++;
                            continue;
                        }

                        // Update common fields
                        location.setName(name);
                        location.setType(locationType);

                        // Optional: OfficialName
                        Integer officialNameCol = columnMap.get(normalizeColumnName(columnConfig.getOfficialName()));
                        if (officialNameCol != null) {
                            String officialName = getCellValueAsString(row.getCell(officialNameCol));
                            if (officialName != null && !officialName.trim().isEmpty()) {
                                location.setOfficialName(officialName.trim());
                            }
                        }

                        // BuildingLocation
                        Integer buildingLocationCol = columnMap.get(normalizeColumnName(columnConfig.getBuildingLocation()));
                        if (buildingLocationCol != null) {
                            String buildingLocationStr = getCellValueAsString(row.getCell(buildingLocationCol));
                            if (buildingLocationStr != null && !buildingLocationStr.trim().isEmpty()) {
                                try {
                                    BuildingLocation buildingLocation = BuildingLocation.valueOf(buildingLocationStr.trim().toUpperCase());
                                    location.setLocation(buildingLocation);
                                } catch (IllegalArgumentException e) {
                                    errors.add("Row " + (i + 1) + ": Invalid BuildingLocation '" + buildingLocationStr + "'. Valid values: LETCANI, TOMESTI");
                                }
                            }
                        }

                        // Mp (square meters)
                        Integer mpCol = columnMap.get(normalizeColumnName(columnConfig.getMp()));
                        if (mpCol != null) {
                            String mpStr = getCellValueAsString(row.getCell(mpCol));
                            if (mpStr != null && !mpStr.trim().isEmpty()) {
                                try {
                                    location.setMp(Integer.parseInt(mpStr.trim()));
                                } catch (NumberFormatException e) {
                                    errors.add("Row " + (i + 1) + ": Invalid mp value '" + mpStr + "'");
                                }
                            }
                        }

                        // Room/RentalSpace specific fields
                        if (location instanceof Room) {
                            Room room = (Room) location;

                            // GroundLevel
                            Integer groundLevelCol = columnMap.get(normalizeColumnName(columnConfig.getGroundLevel()));
                            if (groundLevelCol != null) {
                                String groundLevelStr = getCellValueAsString(row.getCell(groundLevelCol));
                                room.setGroundLevel(parseBoolean(groundLevelStr));
                            }

                            // BuildingName (instead of BuildingId)
                            Integer buildingNameCol = columnMap.get(normalizeColumnName(columnConfig.getBuildingName()));
                            if (buildingNameCol != null) {
                                String buildingName = getCellValueAsString(row.getCell(buildingNameCol));
                                if (buildingName != null && !buildingName.trim().isEmpty()) {
                                    Building building = findBuildingByName(buildingName.trim());
                                    if (building != null) {
                                        room.setBuilding(building);
                                    } else {
                                        errors.add("Row " + (i + 1) + ": Building with name '" + buildingName + "' not found");
                                    }
                                }
                            }
                        }

                        // Save location
                        if (location instanceof Building) {
                            buildingRepository.save((Building) location);
                        } else if (location instanceof RentalSpace) {
                            rentalSpaceRepository.save((RentalSpace) location);
                        } else if (location instanceof Room) {
                            roomRepository.save((Room) location);
                        }

                        if (isUpdate) {
                            updated++;
                        } else {
                            created++;
                        }

                    } catch (Exception e) {
                        log.error("Error processing row " + (i + 1), e);
                        errors.add("Row " + (i + 1) + ": " + e.getMessage());
                        skipped++;
                    }
                }
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }

        LocationImportResultDto result = new LocationImportResultDto();
        result.setTotalRows(totalRows);
        result.setCreated(created);
        result.setUpdated(updated);
        result.setSkipped(skipped);
        result.setErrors(errors);

        log.info("Location import completed: {} created, {} updated, {} skipped", created, updated, skipped);

        return result;
    }

    /**
     * Export all locations to Excel file.
     */
    public byte[] exportLocationsToExcel() throws IOException {
        List<Location> allLocations = locationRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Locations");

            // Create header row with configurable column names
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    columnConfig.getLocationType(),
                    columnConfig.getName(),
                    columnConfig.getOfficialName(),
                    columnConfig.getBuildingLocation(),
                    columnConfig.getMp(),
                    columnConfig.getGroundLevel(),
                    columnConfig.getBuildingName()
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);

                // Style header
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }

            // Fill data rows
            int rowNum = 1;
            for (Location location : allLocations) {
                Row row = sheet.createRow(rowNum++);

                // LocationType
                row.createCell(0).setCellValue(location.getType() != null ? location.getType().name() : "");

                // Name
                row.createCell(1).setCellValue(location.getName() != null ? location.getName() : "");

                // OfficialName
                row.createCell(2).setCellValue(location.getOfficialName() != null ? location.getOfficialName() : "");

                // BuildingLocation
                row.createCell(3).setCellValue(location.getLocation() != null ? location.getLocation().name() : "");

                // Mp
                row.createCell(4).setCellValue(location.getMp() != null ? location.getMp().toString() : "");

                // GroundLevel (only for Room/RentalSpace)
                if (location instanceof Room room) {
                    row.createCell(5).setCellValue(room.getGroundLevel() != null ? room.getGroundLevel().toString() : "");

                    // BuildingName
                    if (room.getBuilding() != null) {
                        row.createCell(6).setCellValue(room.getBuilding().getName());
                    } else {
                        row.createCell(6).setCellValue("");
                    }
                } else {
                    row.createCell(5).setCellValue("");
                    row.createCell(6).setCellValue("");
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // Helper methods

    private Building findBuildingByName(String name) {
        return buildingRepository.findAll().stream()
                .filter(b -> b.getName() != null && b.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private Room findRoomByName(String name) {
        return roomRepository.findAll().stream()
                .filter(r -> r.getName() != null && r.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private RentalSpace findRentalSpaceByName(String name) {
        return rentalSpaceRepository.findAll().stream()
                .filter(rs -> rs.getName() != null && rs.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String header = getCellValueAsString(cell);
                if (header != null && !header.trim().isEmpty()) {
                    String normalized = normalizeColumnName(header);
                    map.put(normalized, i);
                }
            }
        }
        return map;
    }

    private String normalizeColumnName(String name) {
        return name.trim().toLowerCase()
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals("true") ||
               normalized.equals("yes") ||
               normalized.equals("da") ||
               normalized.equals("1") ||
               normalized.equals("x");
    }
}

