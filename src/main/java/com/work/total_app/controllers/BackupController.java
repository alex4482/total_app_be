package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.backup.BackupInfoDto;
import com.work.total_app.models.backup.BackupType;
import com.work.total_app.models.backup.RestoreRequestDto;
import com.work.total_app.repositories.BackupMetadataRepository;
import com.work.total_app.services.BackupService;
import com.work.total_app.services.GoogleDriveService;
import com.work.total_app.services.RestoreService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * REST Controller for database backup and restore operations.
 */
@RestController
@RequestMapping("/backups")
@Log4j2
public class BackupController {

    @Autowired
    private BackupService backupService;
    
    @Autowired
    private RestoreService restoreService;
    
    @Autowired
    private GoogleDriveService googleDriveService;
    
    @Autowired
    private BackupMetadataRepository backupMetadataRepo;
    
    /**
     * Get list of all backups with basic metadata.
     * GET /api/backups
     * 
     * @return List of backup information
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BackupInfoDto>>> listBackups() {
        try {
            List<BackupInfoDto> backups = backupService.listBackups();
            return ResponseEntity.ok(ApiResponse.success(backups));
        } catch (Exception e) {
            log.error("Error listing backups: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to list backups: " + e.getMessage()));
        }
    }
    
    /**
     * Get details of a specific backup.
     * GET /api/backups/{backupName}
     * 
     * @param backupName Name of the backup
     * @return Backup information
     */
    @GetMapping("/{backupName}")
    public ResponseEntity<ApiResponse<BackupInfoDto>> getBackup(@PathVariable String backupName) {
        try {
            BackupInfoDto backup = backupService.getBackupByName(backupName);
            if (backup == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Backup not found: " + backupName));
            }
            return ResponseEntity.ok(ApiResponse.success(backup));
        } catch (Exception e) {
            log.error("Error getting backup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get backup: " + e.getMessage()));
        }
    }
    
    /**
     * Create a manual backup.
     * POST /api/backups/create
     * 
     * Query parameters:
     * - format: "zip" (default) or "excel" - determines what is returned
     * - description: Optional description for the backup
     * 
     * @param format Format to return (zip or excel)
     * @param description Optional description
     * @param returnFile If true, returns the file; if false, returns metadata only
     * @return Backup file or metadata
     */
    @PostMapping
    public ResponseEntity<?> createManualBackup(
            @RequestParam(defaultValue = "zip") String format,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") boolean returnFile) {
        
        try {
            log.info("Creating manual backup with format: {}", format);
            
            // Create backup
            BackupInfoDto backupInfo = backupService.createBackup(
                BackupType.MANUAL, 
                description != null ? description : "Manual backup"
            );
            
            log.info("Manual backup created: {}", backupInfo.getBackupName());
            
            // Upload to Google Drive if configured
            if (googleDriveService.isConfigured()) {
                Path zipPath = Paths.get(backupInfo.getLocalPath());
                String driveFileId = googleDriveService.uploadToGoogleDrive(
                    zipPath, 
                    backupInfo.getBackupName() + ".zip"
                );
                
                if (driveFileId != null) {
                    backupMetadataRepo.findById(backupInfo.getId()).ifPresent(meta -> {
                        meta.setGoogleDriveFileId(driveFileId);
                        backupMetadataRepo.save(meta);
                    });
                    backupInfo.setGoogleDriveFileId(driveFileId);
                    log.info("Backup uploaded to Google Drive: {}", driveFileId);
                }
            }
            
            // Return file if requested
            if (returnFile) {
                if ("excel".equalsIgnoreCase(format)) {
                    return downloadBackupExcel(backupInfo.getBackupName());
                } else {
                    return downloadBackupZip(backupInfo.getBackupName());
                }
            } else {
                return ResponseEntity.ok(ApiResponse.success(backupInfo));
            }
            
        } catch (Exception e) {
            log.error("Error creating manual backup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create backup: " + e.getMessage()));
        }
    }
    
    /**
     * Download backup as ZIP file.
     * GET /api/backups/{backupName}/download/zip
     * 
     * @param backupName Name of the backup
     * @return ZIP file
     */
    @GetMapping("/{backupName}/download/zip")
    public ResponseEntity<?> downloadBackupZip(@PathVariable String backupName) {
        try {
            Path zipPath = backupService.getBackupZipPath(backupName);
            
            if (zipPath == null || !Files.exists(zipPath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Backup ZIP file not found: " + backupName));
            }
            
            Resource resource = new FileSystemResource(zipPath);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + backupName + ".zip\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error downloading backup ZIP: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to download backup: " + e.getMessage()));
        }
    }
    
    /**
     * Download backup as Excel file.
     * GET /api/backups/{backupName}/download/excel
     * 
     * @param backupName Name of the backup
     * @return Excel file
     */
    @GetMapping("/{backupName}/download/excel")
    public ResponseEntity<?> downloadBackupExcel(@PathVariable String backupName) {
        try {
            Path excelPath = backupService.getBackupExcelPath(backupName);
            
            if (excelPath == null || !Files.exists(excelPath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Backup Excel file not found: " + backupName));
            }
            
            Resource resource = new FileSystemResource(excelPath);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + backupName + ".xlsx\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error downloading backup Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to download backup: " + e.getMessage()));
        }
    }
    
    /**
     * Download backup as ZIP with Excel (instead of JSON).
     * Creates a temporary ZIP with "BAZA DATE" folder containing only Excel + "FISIERE" folder.
     * GET /api/backups/{backupName}/download/excel-zip
     * 
     * @param backupName Name of the backup
     * @return ZIP file with Excel + Files
     */
    @GetMapping("/{backupName}/download/excel-zip")
    public ResponseEntity<?> downloadBackupExcelZip(@PathVariable String backupName) {
        try {
            Path backupDir = backupService.getBackupDirPath(backupName);
            
            if (backupDir == null || !Files.exists(backupDir)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Backup not found: " + backupName));
            }
            
            // Create temporary ZIP with Excel + Files
            Path tempZip = Files.createTempFile("backup_excel_", ".zip");
            
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(tempZip.toFile()))) {
                
                // Add Excel file from "BAZA DATE" folder
                Path excelPath = backupDir.resolve("BAZA DATE").resolve("backup.xlsx");
                if (Files.exists(excelPath)) {
                    addToZip(zos, excelPath, "BAZA DATE/backup.xlsx");
                }
                
                // Add all files from "FISIERE" folder
                Path filesDir = backupDir.resolve("FISIERE");
                if (Files.exists(filesDir)) {
                    Files.walk(filesDir)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            try {
                                String relativePath = "FISIERE/" + filesDir.relativize(path).toString();
                                addToZip(zos, path, relativePath);
                            } catch (Exception e) {
                                log.error("Failed to add file to ZIP: {}", path, e);
                            }
                        });
                }
            }
            
            Resource resource = new FileSystemResource(tempZip);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + backupName + "_excel.zip\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error creating Excel ZIP: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create Excel ZIP: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to add file to ZIP.
     */
    private void addToZip(java.util.zip.ZipOutputStream zos, Path file, String entryName) throws Exception {
        java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(entryName.replace("\\", "/"));
        zos.putNextEntry(zipEntry);
        Files.copy(file, zos);
        zos.closeEntry();
    }
    
    /**
     * Restore database from a backup.
     * POST /api/backups/restore
     * 
     * Request body:
     * {
     *   "backupName": "backup_2025-11-03_14-30-15_manual",
     *   "fromExcel": false
     * }
     * 
     * @param request Restore request with backup name and format
     * @return Success or error message
     */
    @PostMapping("/restore")
    public ResponseEntity<ApiResponse<String>> restoreBackup(@RequestBody RestoreRequestDto request) {
        try {
            log.warn("Restoring database from backup: {}", request.getBackupName());
            
            String result;
            
            if (Boolean.TRUE.equals(request.getFromExcel())) {
                // Restore from Excel
                Path excelPath = backupService.getBackupExcelPath(request.getBackupName());
                if (excelPath == null || !Files.exists(excelPath)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Backup Excel file not found: " + request.getBackupName()));
                }
                result = restoreService.restoreFromExcel(excelPath);
            } else {
                // Restore from JSON (in ZIP)
                result = restoreService.restoreFromBackup(request.getBackupName());
            }
            
            log.info("Database restored successfully from: {}", request.getBackupName());
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (IllegalArgumentException e) {
            log.error("Backup not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error restoring backup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to restore backup: " + e.getMessage()));
        }
    }
    
    /**
     * Delete a backup.
     * DELETE /api/backups/{backupName}
     * 
     * @param backupName Name of the backup to delete
     * @return Success or error message
     */
    @DeleteMapping("/{backupName}")
    public ResponseEntity<ApiResponse<String>> deleteBackup(@PathVariable String backupName) {
        try {
            BackupInfoDto backup = backupService.getBackupByName(backupName);
            if (backup == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Backup not found: " + backupName));
            }
            
            // Delete from Google Drive if exists
            if (backup.getGoogleDriveFileId() != null) {
                googleDriveService.deleteFromGoogleDrive(backup.getGoogleDriveFileId());
            }
            
            // Delete local files
            Path zipPath = Paths.get(backup.getLocalPath());
            if (Files.exists(zipPath)) {
                Files.delete(zipPath);
            }
            
            Path backupDir = zipPath.getParent().resolve(backupName);
            if (Files.exists(backupDir)) {
                Files.walk(backupDir)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            log.warn("Failed to delete: {}", path);
                        }
                    });
            }
            
            // Delete metadata
            backupMetadataRepo.findByBackupName(backupName).ifPresent(backupMetadataRepo::delete);
            
            log.info("Backup deleted: {}", backupName);
            return ResponseEntity.ok(ApiResponse.success("Backup deleted successfully"));
            
        } catch (Exception e) {
            log.error("Error deleting backup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete backup: " + e.getMessage()));
        }
    }
}

