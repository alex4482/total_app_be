package com.work.total_app.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.work.total_app.models.backup.BackupInfoDto;
import com.work.total_app.models.backup.BackupMetadata;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for creating database backups.
 * Supports JSON serialization and Excel export.
 */
@Service
@Log4j2
public class BackupService {

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
    
    @Value("${app.backup.local-dir:/DATA/backups}")
    private String backupLocalDir;
    
    @Value("${app.storage.baseDir:/FISIERE}")
    private String storageBaseDir;
    
    private final ObjectMapper objectMapper;
    
    public BackupService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Create a backup with JSON and Excel files.
     * 
     * @param backupType MANUAL or AUTOMATIC
     * @param description Optional description
     * @return BackupInfoDto with backup details
     */
    @Transactional
    public BackupInfoDto createBackup(BackupType backupType, String description) throws Exception {
        log.info("Starting backup creation: type={}, description={}", backupType, description);
        
        // Generate backup name with timestamp
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        String backupName = "backup_" + timestamp + "_" + backupType.name().toLowerCase();
        
        // Create backup directory
        Path backupDir = Paths.get(backupLocalDir, backupName);
        Files.createDirectories(backupDir);
        
        // Create "BAZA DATE" folder for database exports
        Path databaseDir = backupDir.resolve("BAZA DATE");
        Files.createDirectories(databaseDir);
        
        // Export to JSON (in BAZA DATE folder)
        exportToJson(databaseDir);
        
        // Export to Excel (in BAZA DATE folder)
        exportToExcel(databaseDir);
        
        // Create "FISIERE" folder for physical files
        Path filesDir = backupDir.resolve("FISIERE");
        Files.createDirectories(filesDir);
        
        // Backup physical files (in FISIERE folder)
        backupPhysicalFiles(filesDir);
        
        // Create ZIP file
        Path zipPath = Paths.get(backupLocalDir, backupName + ".zip");
        createZipFromDirectory(backupDir, zipPath);
        
        // Calculate size
        long zipSize = Files.size(zipPath);
        
        // Save metadata to database
        BackupMetadata metadata = new BackupMetadata();
        metadata.setBackupName(backupName);
        metadata.setBackupType(backupType);
        metadata.setLocalPath(zipPath.toString());
        metadata.setSizeBytes(zipSize);
        metadata.setDescription(description);
        metadata = backupMetadataRepo.save(metadata);
        
        log.info("Backup created successfully: {}", backupName);
        
        return toDto(metadata);
    }
    
    /**
     * Export all entities to JSON files.
     */
    private void exportToJson(Path backupDir) throws Exception {
        log.debug("Exporting to JSON...");
        
        Path jsonDir = backupDir.resolve("json");
        Files.createDirectories(jsonDir);
        
        // Export each entity type
        writeJson(jsonDir.resolve("tenants.json"), tenantRepo.findAll());
        writeJson(jsonDir.resolve("buildings.json"), buildingRepo.findAll());
        writeJson(jsonDir.resolve("rooms.json"), roomRepo.findAll());
        writeJson(jsonDir.resolve("rental_spaces.json"), rentalSpaceRepo.findAll());
        writeJson(jsonDir.resolve("index_counters.json"), indexCounterRepo.findAll());
        writeJson(jsonDir.resolve("index_data.json"), indexDataRepo.findAll());
        writeJson(jsonDir.resolve("email_presets.json"), emailPresetRepo.findAll());
        writeJson(jsonDir.resolve("file_assets.json"), fileAssetRepo.findAll());
        writeJson(jsonDir.resolve("tenant_rental_data.json"), tenantRentalDataRepo.findAll());
        
        log.debug("JSON export completed");
    }
    
    /**
     * Export all entities to Excel file (multiple sheets).
     */
    private void exportToExcel(Path backupDir) throws Exception {
        log.debug("Exporting to Excel...");
        
        Path excelPath = backupDir.resolve("backup.xlsx");
        
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create sheets for each entity
            createTenantSheet(workbook);
            createBuildingSheet(workbook);
            createRoomSheet(workbook);
            createRentalSpaceSheet(workbook);
            createIndexCounterSheet(workbook);
            createIndexDataSheet(workbook);
            createEmailPresetSheet(workbook);
            createTenantRentalDataSheet(workbook);
            createFileAssetSheet(workbook);
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                workbook.write(fos);
            }
        }
        
        log.debug("Excel export completed");
    }
    
    private void createTenantSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Chiriasi");
        
        // Header
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Name", "CUI", "Reg Number", "PF", "Active", "Emails", "Phone Numbers", "Observations"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        // Data
        List<Tenant> tenants = tenantRepo.findAll();
        int rowNum = 1;
        for (Tenant tenant : tenants) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(tenant.getId() != null ? tenant.getId() : 0L);
            row.createCell(1).setCellValue(tenant.getName());
            row.createCell(2).setCellValue(tenant.getCui());
            row.createCell(3).setCellValue(tenant.getRegNumber());
            row.createCell(4).setCellValue(tenant.getPf() != null ? tenant.getPf() : false);
            row.createCell(5).setCellValue(tenant.getActive() != null ? tenant.getActive() : false);
            row.createCell(6).setCellValue(tenant.getEmails() != null ? String.join(";", tenant.getEmails()) : "");
            row.createCell(7).setCellValue(tenant.getPhoneNumbers() != null ? String.join(";", tenant.getPhoneNumbers()) : "");
            row.createCell(8).setCellValue(tenant.getObservations() != null ? tenant.getObservations().toString() : "");
        }
        
        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createBuildingSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Cladiri");
        
        // Header
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Name", "Official Name", "Location", "MP", "Type", "Observations"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        // Data
        List<Building> buildings = buildingRepo.findAll();
        int rowNum = 1;
        for (Building building : buildings) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(building.getId() != null ? building.getId() : 0L);
            row.createCell(1).setCellValue(building.getName());
            row.createCell(2).setCellValue(building.getOfficialName());
            row.createCell(3).setCellValue(building.getLocation() != null ? building.getLocation().name() : "");
            row.createCell(4).setCellValue(building.getMp() != null ? building.getMp() : 0);
            row.createCell(5).setCellValue(building.getType() != null ? building.getType().name() : "");
            row.createCell(6).setCellValue(building.getObservations() != null ? building.getObservations().toString() : "");
        }
        
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createRoomSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Camere");
        
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Name", "Official Name", "Building ID", "Location", "MP", "Ground Level", "Type", "Observations"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        List<Room> rooms = roomRepo.findAll();
        int rowNum = 1;
        for (Room room : rooms) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(room.getId() != null ? room.getId() : 0L);
            row.createCell(1).setCellValue(room.getName());
            row.createCell(2).setCellValue(room.getOfficialName());
            row.createCell(3).setCellValue(room.getBuilding() != null ? room.getBuilding().getId() : 0L);
            row.createCell(4).setCellValue(room.getLocation() != null ? room.getLocation().name() : "");
            row.createCell(5).setCellValue(room.getMp() != null ? room.getMp() : 0);
            row.createCell(6).setCellValue(room.getGroundLevel() != null ? room.getGroundLevel() : false);
            row.createCell(7).setCellValue(room.getType() != null ? room.getType().name() : "");
            row.createCell(8).setCellValue(room.getObservations() != null ? room.getObservations().toString() : "");
        }
        
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createRentalSpaceSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Spatii chirie");
        
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Name", "Official Name", "Building ID", "Location", "MP", "Type", "Observations"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        List<RentalSpace> rentalSpaces = rentalSpaceRepo.findAll();
        int rowNum = 1;
        for (RentalSpace space : rentalSpaces) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(space.getId() != null ? space.getId() : 0L);
            row.createCell(1).setCellValue(space.getName());
            row.createCell(2).setCellValue(space.getOfficialName());
            row.createCell(3).setCellValue(space.getBuilding() != null ? space.getBuilding().getId() : 0L);
            row.createCell(4).setCellValue(space.getLocation() != null ? space.getLocation().name() : "");
            row.createCell(5).setCellValue(space.getMp() != null ? space.getMp() : 0);
            row.createCell(6).setCellValue(space.getType() != null ? space.getType().name() : "");
            row.createCell(7).setCellValue(space.getObservations() != null ? space.getObservations().toString() : "");
        }
        
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createIndexCounterSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Contori");
        
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Name", "Location ID", "Counter Type", "Location Type", "Building Location"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        List<IndexCounter> counters = indexCounterRepo.findAll();
        int rowNum = 1;
        for (IndexCounter counter : counters) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(counter.getId() != null ? counter.getId() : 0L);
            row.createCell(1).setCellValue(counter.getName());
            row.createCell(2).setCellValue(counter.getLocation() != null ? counter.getLocation().getId() : 0L);
            row.createCell(3).setCellValue(counter.getCounterType() != null ? counter.getCounterType().name() : "");
            row.createCell(4).setCellValue(counter.getLocationType() != null ? counter.getLocationType().name() : "");
            row.createCell(5).setCellValue(counter.getBuildingLocation() != null ? counter.getBuildingLocation().name() : "");
        }
        
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createIndexDataSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("date contori");
        
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Counter ID", "Index", "Consumption", "Type", "Reading Date"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        List<IndexData> indexDataList = indexDataRepo.findAll();
        int rowNum = 1;
        for (IndexData data : indexDataList) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data.getId() != null ? data.getId() : 0L);
            row.createCell(1).setCellValue(data.getCounter() != null ? data.getCounter().getId() : 0L);
            row.createCell(2).setCellValue(data.getIndex() != null ? data.getIndex() : 0.0);
            row.createCell(3).setCellValue(data.getConsumption() != null ? data.getConsumption() : 0.0);
            row.createCell(4).setCellValue(data.getType() != null ? data.getType().name() : "");
            row.createCell(5).setCellValue(data.getReadingDate() != null ? data.getReadingDate().toString() : "");
        }
        
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createEmailPresetSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("preseturi email");
        
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Name", "Subject", "Message", "Recipients", "Keywords"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        List<EmailPreset> presets = emailPresetRepo.findAll();
        int rowNum = 1;
        for (EmailPreset preset : presets) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(preset.getId() != null ? preset.getId() : 0L);
            row.createCell(1).setCellValue(preset.getName());
            row.createCell(2).setCellValue(preset.getSubject());
            row.createCell(3).setCellValue(preset.getMessage());
            row.createCell(4).setCellValue(preset.getRecipients() != null ? String.join(";", preset.getRecipients()) : "");
            row.createCell(5).setCellValue(preset.getKeywords() != null ? String.join(";", preset.getKeywords()) : "");
        }
        
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createTenantRentalDataSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("contracte-inchirieri");
        
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Tenant ID", "Rental Space ID", "Start Date", "End Date", "Rent"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        List<TenantRentalData> rentalDataList = tenantRentalDataRepo.findAll();
        int rowNum = 1;
        for (TenantRentalData data : rentalDataList) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data.getId() != null ? data.getId() : 0L);
            row.createCell(1).setCellValue(data.getTenant() != null ? data.getTenant().getId() : 0L);
            row.createCell(2).setCellValue(data.getRentalSpace() != null ? data.getRentalSpace().getId() : 0L);
            row.createCell(3).setCellValue(data.getStartDate() != null ? data.getStartDate().toString() : "");
            row.createCell(4).setCellValue(data.getEndDate() != null ? data.getEndDate().toString() : "");
            row.createCell(5).setCellValue(data.getRent() != null ? data.getRent() : 0.0);
        }
        
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createFileAssetSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("fisiere");
        
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Owner Type", "Owner ID", "Filename", "Content Type", "Size Bytes", "Checksum", "Uploaded At"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        List<FileAsset> files = fileAssetRepo.findAll();
        int rowNum = 1;
        for (FileAsset file : files) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(file.getId() != null ? file.getId().toString() : "");
            row.createCell(1).setCellValue(file.getOwnerType() != null ? file.getOwnerType().name() : "");
            row.createCell(2).setCellValue(file.getOwnerId() != null ? file.getOwnerId() : 0L);
            row.createCell(3).setCellValue(file.getOriginalFilename());
            row.createCell(4).setCellValue(file.getContentType());
            row.createCell(5).setCellValue(file.getSizeBytes());
            row.createCell(6).setCellValue(file.getChecksum());
            row.createCell(7).setCellValue(file.getUploadedAt() != null ? file.getUploadedAt().toString() : "");
        }
        
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
    
    private void writeJson(Path file, Object data) throws IOException {
        objectMapper.writeValue(file.toFile(), data);
    }
    
    /**
     * Backup physical files from storage directory.
     * Copies all files from /FISIERE/ to the provided directory.
     */
    private void backupPhysicalFiles(Path targetDir) throws IOException {
        log.debug("Backing up physical files from: {}", storageBaseDir);
        
        Path sourceDir = Paths.get(storageBaseDir);
        if (!Files.exists(sourceDir)) {
            log.warn("Storage directory does not exist: {}. Skipping physical files backup.", storageBaseDir);
            return;
        }
        
        // Copy entire storage directory structure to target
        copyDirectory(sourceDir, targetDir);
        
        long fileCount = Files.walk(targetDir)
            .filter(Files::isRegularFile)
            .count();
        
        log.debug("Physical files backup completed: {} files copied", fileCount);
    }
    
    /**
     * Recursively copy directory with all contents.
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source)
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    log.error("Failed to copy file: {} -> {}", sourcePath, target, e);
                }
            });
    }
    
    /**
     * Create ZIP file from directory.
     */
    private void createZipFromDirectory(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            Files.walk(sourceDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                    try {
                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        log.error("Error adding file to ZIP: {}", path, e);
                    }
                });
        }
    }
    
    /**
     * List all backups.
     */
    public List<BackupInfoDto> listBackups() {
        return backupMetadataRepo.findAll().stream()
                .map(this::toDto)
                .toList();
    }
    
    /**
     * Get backup by name.
     */
    public BackupInfoDto getBackupByName(String backupName) {
        return backupMetadataRepo.findByBackupName(backupName)
                .map(this::toDto)
                .orElse(null);
    }
    
    /**
     * Get backup ZIP file path.
     */
    public Path getBackupZipPath(String backupName) {
        return backupMetadataRepo.findByBackupName(backupName)
                .map(meta -> Paths.get(meta.getLocalPath()))
                .orElse(null);
    }
    
    /**
     * Get backup Excel file path.
     */
    public Path getBackupExcelPath(String backupName) {
        return backupMetadataRepo.findByBackupName(backupName)
                .map(meta -> {
                    Path zipPath = Paths.get(meta.getLocalPath());
                    Path backupDir = zipPath.getParent().resolve(backupName);
                    return backupDir.resolve("BAZA DATE").resolve("backup.xlsx");
                })
                .orElse(null);
    }
    
    /**
     * Get backup directory path (unzipped).
     */
    public Path getBackupDirPath(String backupName) {
        return backupMetadataRepo.findByBackupName(backupName)
                .map(meta -> {
                    Path zipPath = Paths.get(meta.getLocalPath());
                    return zipPath.getParent().resolve(backupName);
                })
                .orElse(null);
    }
    
    private BackupInfoDto toDto(BackupMetadata metadata) {
        return new BackupInfoDto(
                metadata.getId(),
                metadata.getBackupName(),
                metadata.getBackupType().name(),
                metadata.getCreatedAt().toString(),
                metadata.getLocalPath(),
                metadata.getGoogleDriveFileId(),
                metadata.getSizeBytes(),
                metadata.getDescription()
        );
    }
}

