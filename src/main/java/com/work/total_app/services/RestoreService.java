package com.work.total_app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.work.total_app.models.backup.BackupInfoDto;
import com.work.total_app.models.backup.BackupType;
import com.work.total_app.models.building.Building;
import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.building.Room;
import com.work.total_app.models.email.EmailPreset;
import com.work.total_app.models.file.FileAsset;
import com.work.total_app.models.reading.IndexCounter;
import com.work.total_app.models.reading.IndexData;
import com.work.total_app.models.tenant.Tenant;
import com.work.total_app.models.tenant.TenantRentalData;
import com.work.total_app.repositories.*;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for restoring database from backups.
 * Supports both JSON and Excel restoration.
 */
@Service
@Log4j2
public class RestoreService {

    @Autowired
    private TenantRepository tenantRepo;
    
    @Autowired
    private BuildingRepository buildingRepo;
    
    @Autowired
    private RoomRepository roomRepo;
    
    @Autowired
    private RentalSpaceRepository rentalSpaceRepo;
    
    @Autowired
    private IndexCounterRepository indexCounterRepo;
    
    @Autowired
    private IndexDataRepository indexDataRepo;
    
    @Autowired
    private EmailFileKeywordPairRepository emailPresetRepo;
    
    @Autowired
    private FileAssetRepository fileAssetRepo;
    
    @Autowired
    private TenantRentalDataRepository tenantRentalDataRepo;
    
    @Autowired
    private BackupMetadataRepository backupMetadataRepo;
    
    @Autowired
    private BackupService backupService;
    
    @Value("${app.storage.baseDir:/FISIERE}")
    private String storageBaseDir;
    
    private final ObjectMapper objectMapper;
    
    public RestoreService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Restore database from a backup (JSON format) with automatic rollback on failure.
     * 
     * This method creates a safety backup before clearing the database.
     * If restoration fails, it automatically rolls back to the safety backup.
     * 
     * @param backupName Name of the backup to restore
     * @return Success message
     */
    @Transactional
    public String restoreFromBackup(String backupName) throws Exception {
        log.info("Starting restore from backup: {}", backupName);
        
        // Find backup metadata
        var backupMeta = backupMetadataRepo.findByBackupName(backupName)
                .orElseThrow(() -> new IllegalArgumentException("Backup not found: " + backupName));
        
        Path zipPath = Paths.get(backupMeta.getLocalPath());
        if (!Files.exists(zipPath)) {
            throw new IllegalArgumentException("Backup file not found: " + zipPath);
        }
        
        // SAFETY: Create backup before restore (in case restore fails)
        log.warn("Creating safety backup before restore...");
        String safetyBackupName = null;
        try {
            BackupInfoDto safetyBackup = backupService.createBackup(
                BackupType.MANUAL,
                "SAFETY BACKUP before restore from: " + backupName
            );
            safetyBackupName = safetyBackup.getBackupName();
            log.info("Safety backup created: {}", safetyBackupName);
        } catch (Exception e) {
            log.error("Failed to create safety backup: {}", e.getMessage());
            throw new IllegalStateException(
                "Cannot proceed with restore: Failed to create safety backup. " +
                "Your data is safe and unchanged.", e);
        }
        
        Path tempDir = null;
        try {
            // Extract ZIP to temp directory
            tempDir = Files.createTempDirectory("restore_" + backupName);
            extractZip(zipPath, tempDir);
            
            // Clear existing data
            log.warn("Clearing database...");
            clearDatabase();
            
            // Restore from JSON files (in BAZA DATE folder)
            log.info("Restoring data from backup...");
            Path databaseDir = tempDir.resolve("BAZA DATE");
            Path jsonDir = databaseDir.resolve("json");
            restoreFromJson(jsonDir);
            
            // Restore physical files (from FISIERE folder)
            log.info("Restoring physical files...");
            Path filesDir = tempDir.resolve("FISIERE");
            restorePhysicalFiles(filesDir);
            
            log.info("Restore completed successfully from backup: {}", backupName);
            log.info("Safety backup is available if needed: {}", safetyBackupName);
            
            return "Database restored successfully from backup: " + backupName + 
                   ". Safety backup available: " + safetyBackupName;
            
        } catch (Exception e) {
            log.error("RESTORE FAILED: {}", e.getMessage(), e);
            log.warn("Attempting automatic rollback to safety backup...");
            
            try {
                // Rollback to safety backup
                rollbackToSafetyBackup(safetyBackupName);
                log.info("Rollback successful! Database restored to state before restore attempt.");
                
                throw new IllegalStateException(
                    "Restore failed but database was rolled back to previous state. " +
                    "Error: " + e.getMessage(), e);
                
            } catch (Exception rollbackError) {
                log.error("CRITICAL: Rollback also failed: {}", rollbackError.getMessage(), rollbackError);
                throw new IllegalStateException(
                    "CRITICAL: Restore failed AND rollback failed! " +
                    "Safety backup available at: " + safetyBackupName + 
                    ". Original error: " + e.getMessage() + 
                    ". Rollback error: " + rollbackError.getMessage(), e);
            }
        } finally {
            // Cleanup temp directory
            if (tempDir != null && Files.exists(tempDir)) {
                try {
                    Files.walk(tempDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception ignored) {}
                        });
                } catch (Exception e) {
                    log.warn("Failed to cleanup temp directory: {}", tempDir);
                }
            }
        }
    }
    
    /**
     * Restore database from Excel file with automatic rollback on failure.
     * 
     * This method creates a safety backup before clearing the database.
     * If restoration fails, it automatically rolls back to the safety backup.
     * 
     * @param excelPath Path to Excel file
     * @return Success message
     */
    @Transactional
    public String restoreFromExcel(Path excelPath) throws Exception {
        log.info("Starting restore from Excel: {}", excelPath);
        
        if (!Files.exists(excelPath)) {
            throw new IllegalArgumentException("Excel file not found: " + excelPath);
        }
        
        // SAFETY: Create backup before restore
        log.warn("Creating safety backup before restore...");
        String safetyBackupName = null;
        try {
            var safetyBackup = backupService.createBackup(
                com.work.total_app.models.backup.BackupType.MANUAL,
                "SAFETY BACKUP before restore from Excel"
            );
            safetyBackupName = safetyBackup.getBackupName();
            log.info("Safety backup created: {}", safetyBackupName);
        } catch (Exception e) {
            log.error("Failed to create safety backup: {}", e.getMessage());
            throw new IllegalStateException(
                "Cannot proceed with restore: Failed to create safety backup. " +
                "Your data is safe and unchanged.", e);
        }
        
        try {
            // Clear existing data
            log.warn("Clearing database...");
            clearDatabase();
            
            // Read and restore from Excel
            log.info("Restoring data from Excel...");
            try (Workbook workbook = WorkbookFactory.create(excelPath.toFile())) {
                restoreTenants(workbook.getSheet("Tenants"));
                restoreBuildings(workbook.getSheet("Buildings"));
                restoreRooms(workbook.getSheet("Rooms"));
                restoreRentalSpaces(workbook.getSheet("RentalSpaces"));
                restoreIndexCounters(workbook.getSheet("IndexCounters"));
                restoreIndexData(workbook.getSheet("IndexData"));
                restoreEmailPresets(workbook.getSheet("EmailPresets"));
                restoreTenantRentalData(workbook.getSheet("TenantRentalData"));
                restoreFileAssets(workbook.getSheet("FileAssets"));
            }
            
            log.info("Restore completed successfully from Excel: {}", excelPath);
            log.info("Safety backup is available if needed: {}", safetyBackupName);
            
            return "Database restored successfully from Excel file. Safety backup available: " + safetyBackupName;
            
        } catch (Exception e) {
            log.error("RESTORE FAILED: {}", e.getMessage(), e);
            log.warn("Attempting automatic rollback to safety backup...");
            
            try {
                // Rollback to safety backup
                rollbackToSafetyBackup(safetyBackupName);
                log.info("Rollback successful! Database restored to state before restore attempt.");
                
                throw new IllegalStateException(
                    "Restore failed but database was rolled back to previous state. " +
                    "Error: " + e.getMessage(), e);
                
            } catch (Exception rollbackError) {
                log.error("CRITICAL: Rollback also failed: {}", rollbackError.getMessage(), rollbackError);
                throw new IllegalStateException(
                    "CRITICAL: Restore failed AND rollback failed! " +
                    "Safety backup available at: " + safetyBackupName + 
                    ". Original error: " + e.getMessage() + 
                    ". Rollback error: " + rollbackError.getMessage(), e);
            }
        }
    }
    
    /**
     * Rollback to safety backup in case restore fails.
     * This is called automatically when a restore operation fails.
     * 
     * @param safetyBackupName Name of the safety backup to restore
     */
    private void rollbackToSafetyBackup(String safetyBackupName) throws Exception {
        if (safetyBackupName == null) {
            throw new IllegalStateException("No safety backup available for rollback");
        }
        
        log.warn("ROLLBACK: Restoring from safety backup: {}", safetyBackupName);
        
        // Find safety backup
        var safetyBackup = backupMetadataRepo.findByBackupName(safetyBackupName)
                .orElseThrow(() -> new IllegalStateException("Safety backup not found: " + safetyBackupName));
        
        Path zipPath = Paths.get(safetyBackup.getLocalPath());
        if (!Files.exists(zipPath)) {
            throw new IllegalStateException("Safety backup file not found: " + zipPath);
        }
        
        // Extract and restore
        Path tempDir = Files.createTempDirectory("rollback_" + safetyBackupName);
        try {
            extractZip(zipPath, tempDir);
            clearDatabase();
            
            // Restore from JSON (in BAZA DATE folder)
            Path databaseDir = tempDir.resolve("BAZA DATE");
            Path jsonDir = databaseDir.resolve("json");
            restoreFromJson(jsonDir);
            
            // Restore physical files (from FISIERE folder)
            Path filesDir = tempDir.resolve("FISIERE");
            restorePhysicalFiles(filesDir);
            
            log.info("Rollback completed successfully");
        } finally {
            // Cleanup
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception ignored) {}
                    });
            }
        }
    }
    
    /**
     * Clear all data from database.
     */
    private void clearDatabase() {
        log.warn("Clearing all database tables...");
        
        // Delete in correct order to respect foreign key constraints
        indexDataRepo.deleteAll();
        indexCounterRepo.deleteAll();
        tenantRentalDataRepo.deleteAll();
        rentalSpaceRepo.deleteAll();
        roomRepo.deleteAll();
        buildingRepo.deleteAll();
        tenantRepo.deleteAll();
        emailPresetRepo.deleteAll();
        fileAssetRepo.deleteAll();
        
        log.info("Database cleared");
    }
    
    /**
     * Restore from JSON files.
     */
    private void restoreFromJson(Path jsonDir) throws IOException {
        log.debug("Restoring from JSON files...");
        
        // Restore in correct order to respect foreign key constraints
        List<Tenant> tenants = readJson(jsonDir.resolve("tenants.json"), new TypeReference<List<Tenant>>() {});
        tenantRepo.saveAll(tenants);
        
        List<Building> buildings = readJson(jsonDir.resolve("buildings.json"), new TypeReference<List<Building>>() {});
        buildingRepo.saveAll(buildings);
        
        List<Room> rooms = readJson(jsonDir.resolve("rooms.json"), new TypeReference<List<Room>>() {});
        roomRepo.saveAll(rooms);
        
        List<RentalSpace> rentalSpaces = readJson(jsonDir.resolve("rental_spaces.json"), new TypeReference<List<RentalSpace>>() {});
        rentalSpaceRepo.saveAll(rentalSpaces);
        
        List<IndexCounter> indexCounters = readJson(jsonDir.resolve("index_counters.json"), new TypeReference<List<IndexCounter>>() {});
        indexCounterRepo.saveAll(indexCounters);
        
        List<IndexData> indexData = readJson(jsonDir.resolve("index_data.json"), new TypeReference<List<IndexData>>() {});
        indexDataRepo.saveAll(indexData);
        
        List<EmailPreset> emailPresets = readJson(jsonDir.resolve("email_presets.json"), new TypeReference<List<EmailPreset>>() {});
        emailPresetRepo.saveAll(emailPresets);
        
        List<FileAsset> fileAssets = readJson(jsonDir.resolve("file_assets.json"), new TypeReference<List<FileAsset>>() {});
        fileAssetRepo.saveAll(fileAssets);
        
        List<TenantRentalData> tenantRentalData = readJson(jsonDir.resolve("tenant_rental_data.json"), new TypeReference<List<TenantRentalData>>() {});
        tenantRentalDataRepo.saveAll(tenantRentalData);
        
        log.debug("JSON restore completed");
    }
    
    private <T> T readJson(Path file, TypeReference<T> typeRef) throws IOException {
        return objectMapper.readValue(file.toFile(), typeRef);
    }
    
    /**
     * Restore physical files from backup.
     * Clears existing storage directory and restores files from backup.
     */
    private void restorePhysicalFiles(Path filesDir) throws IOException {
        if (!Files.exists(filesDir)) {
            log.warn("No physical files directory in backup. Skipping file restoration.");
            return;
        }
        
        Path storageDir = Paths.get(storageBaseDir);
        
        // Clear existing files (be careful!)
        if (Files.exists(storageDir)) {
            log.warn("Clearing existing storage directory: {}", storageDir);
            Files.walk(storageDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        if (!path.equals(storageDir)) { // Don't delete the root directory
                            Files.delete(path);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to delete: {}", path);
                    }
                });
        }
        
        // Create storage directory if it doesn't exist
        Files.createDirectories(storageDir);
        
        // Copy files from backup
        copyDirectory(filesDir, storageDir);
        
        long fileCount = Files.walk(storageDir)
            .filter(Files::isRegularFile)
            .count();
        
        log.info("Restored {} physical files to: {}", fileCount, storageDir);
    }
    
    /**
     * Recursively copy directory with all contents.
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source)
            .forEach(sourcePath -> {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                try {
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    log.error("Failed to copy file: {} -> {}", sourcePath, targetPath, e);
                }
            });
    }
    
    /**
     * Extract ZIP file to directory.
     */
    private void extractZip(Path zipPath, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = destDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
    
    // Excel restoration methods
    
    private void restoreTenants(Sheet sheet) {
        if (sheet == null) return;
        
        List<Tenant> tenants = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Tenant tenant = new Tenant();
            tenant.setId(getLongValue(row.getCell(0)));
            tenant.setName(getStringValue(row.getCell(1)));
            tenant.setCui(getStringValue(row.getCell(2)));
            tenant.setRegNumber(getStringValue(row.getCell(3)));
            tenant.setPf(getBooleanValue(row.getCell(4)));
            tenant.setActive(getBooleanValue(row.getCell(5)));
            
            String emails = getStringValue(row.getCell(6));
            if (emails != null && !emails.isEmpty()) {
                tenant.setEmails(List.of(emails.split(";")));
            }
            
            String phones = getStringValue(row.getCell(7));
            if (phones != null && !phones.isEmpty()) {
                tenant.setPhoneNumbers(List.of(phones.split(";")));
            }
            
            tenants.add(tenant);
        }
        
        tenantRepo.saveAll(tenants);
        log.debug("Restored {} tenants from Excel", tenants.size());
    }
    
    private void restoreBuildings(Sheet sheet) {
        if (sheet == null) return;
        
        List<Building> buildings = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Building building = new Building();
            building.setId(getLongValue(row.getCell(0)));
            building.setName(getStringValue(row.getCell(1)));
            building.setOfficialName(getStringValue(row.getCell(2)));
            // Note: You'll need to properly restore location, type, etc. based on your enums
            building.setMp(getIntValue(row.getCell(4)));
            
            buildings.add(building);
        }
        
        buildingRepo.saveAll(buildings);
        log.debug("Restored {} buildings from Excel", buildings.size());
    }
    
    private void restoreRooms(Sheet sheet) {
        if (sheet == null) return;
        
        List<Room> rooms = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Room room = new Room();
            room.setId(getLongValue(row.getCell(0)));
            room.setName(getStringValue(row.getCell(1)));
            room.setOfficialName(getStringValue(row.getCell(2)));
            room.setMp(getIntValue(row.getCell(5)));
            room.setGroundLevel(getBooleanValue(row.getCell(6)));
            
            // Restore building relationship
            Long buildingId = getLongValue(row.getCell(3));
            if (buildingId != null && buildingId > 0) {
                buildingRepo.findById(buildingId).ifPresent(room::setBuilding);
            }
            
            rooms.add(room);
        }
        
        roomRepo.saveAll(rooms);
        log.debug("Restored {} rooms from Excel", rooms.size());
    }
    
    private void restoreRentalSpaces(Sheet sheet) {
        if (sheet == null) return;
        
        List<RentalSpace> rentalSpaces = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            RentalSpace space = new RentalSpace();
            space.setId(getLongValue(row.getCell(0)));
            space.setName(getStringValue(row.getCell(1)));
            space.setOfficialName(getStringValue(row.getCell(2)));
            space.setMp(getIntValue(row.getCell(5)));
            
            // Restore building relationship
            Long buildingId = getLongValue(row.getCell(3));
            if (buildingId != null && buildingId > 0) {
                buildingRepo.findById(buildingId).ifPresent(space::setBuilding);
            }
            
            rentalSpaces.add(space);
        }
        
        rentalSpaceRepo.saveAll(rentalSpaces);
        log.debug("Restored {} rental spaces from Excel", rentalSpaces.size());
    }
    
    private void restoreIndexCounters(Sheet sheet) {
        if (sheet == null) return;
        
        List<IndexCounter> counters = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            IndexCounter counter = new IndexCounter();
            counter.setId(getLongValue(row.getCell(0)));
            counter.setName(getStringValue(row.getCell(1)));
            
            counters.add(counter);
        }
        
        indexCounterRepo.saveAll(counters);
        log.debug("Restored {} index counters from Excel", counters.size());
    }
    
    private void restoreIndexData(Sheet sheet) {
        if (sheet == null) return;
        
        List<IndexData> dataList = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            IndexData data = new IndexData();
            data.setId(getLongValue(row.getCell(0)));
            data.setIndex(getDoubleValue(row.getCell(2)));
            data.setConsumption(getDoubleValue(row.getCell(3)));
            
            // Restore counter relationship
            Long counterId = getLongValue(row.getCell(1));
            if (counterId != null && counterId > 0) {
                indexCounterRepo.findById(counterId).ifPresent(data::setCounter);
            }
            
            dataList.add(data);
        }
        
        indexDataRepo.saveAll(dataList);
        log.debug("Restored {} index data from Excel", dataList.size());
    }
    
    private void restoreEmailPresets(Sheet sheet) {
        if (sheet == null) return;
        
        List<EmailPreset> presets = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            EmailPreset preset = new EmailPreset();
            preset.setId(getLongValue(row.getCell(0)));
            preset.setName(getStringValue(row.getCell(1)));
            preset.setSubject(getStringValue(row.getCell(2)));
            preset.setMessage(getStringValue(row.getCell(3)));
            
            String recipients = getStringValue(row.getCell(4));
            if (recipients != null && !recipients.isEmpty()) {
                preset.setRecipients(recipients.split(";"));
            }
            
            String keywords = getStringValue(row.getCell(5));
            if (keywords != null && !keywords.isEmpty()) {
                preset.setKeywords(keywords.split(";"));
            }
            
            presets.add(preset);
        }
        
        emailPresetRepo.saveAll(presets);
        log.debug("Restored {} email presets from Excel", presets.size());
    }
    
    private void restoreTenantRentalData(Sheet sheet) {
        if (sheet == null) return;
        
        List<TenantRentalData> dataList = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            TenantRentalData data = new TenantRentalData();
            data.setId(getLongValue(row.getCell(0)));
            data.setRent(getDoubleValue(row.getCell(5)));
            
            // Restore tenant relationship
            Long tenantId = getLongValue(row.getCell(1));
            if (tenantId != null && tenantId > 0) {
                tenantRepo.findById(tenantId).ifPresent(data::setTenant);
            }
            
            // Restore rental space relationship
            Long spaceId = getLongValue(row.getCell(2));
            if (spaceId != null && spaceId > 0) {
                rentalSpaceRepo.findById(spaceId).ifPresent(data::setRentalSpace);
            }
            
            dataList.add(data);
        }
        
        tenantRentalDataRepo.saveAll(dataList);
        log.debug("Restored {} tenant rental data from Excel", dataList.size());
    }
    
    private void restoreFileAssets(Sheet sheet) {
        if (sheet == null) return;
        
        List<FileAsset> files = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            FileAsset file = new FileAsset();
            // Note: FileAsset restoration from Excel is limited
            // You may want to skip this or handle it specially
            file.setOriginalFilename(getStringValue(row.getCell(3)));
            file.setContentType(getStringValue(row.getCell(4)));
            file.setSizeBytes(getLongValue(row.getCell(5)));
            file.setChecksum(getStringValue(row.getCell(6)));
            
            files.add(file);
        }
        
        // fileAssetRepo.saveAll(files); // Consider if you want to restore files
        log.debug("Skipped {} file assets from Excel (metadata only)", files.size());
    }
    
    // Helper methods to extract cell values
    
    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
    
    private Long getLongValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (long) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Long.parseLong(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }
    
    private Integer getIntValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }
    
    private Double getDoubleValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }
    
    private Boolean getBooleanValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case STRING -> Boolean.parseBoolean(cell.getStringCellValue());
            case NUMERIC -> cell.getNumericCellValue() != 0;
            default -> null;
        };
    }
}

