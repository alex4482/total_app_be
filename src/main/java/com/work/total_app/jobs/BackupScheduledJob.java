package com.work.total_app.jobs;

import com.work.total_app.models.backup.BackupInfoDto;
import com.work.total_app.models.backup.BackupType;
import com.work.total_app.repositories.BackupMetadataRepository;
import com.work.total_app.services.BackupService;
import com.work.total_app.services.GoogleDriveService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Scheduled job for automatic database backups.
 * Runs every 5 days and uploads to Google Drive if configured.
 */
@Component
@Log4j2
public class BackupScheduledJob {

    @Autowired
    private BackupService backupService;
    
    @Autowired
    private GoogleDriveService googleDriveService;
    
    @Autowired
    private BackupMetadataRepository backupMetadataRepo;
    
    @Scheduled(cron = "0 0 2 */5 * ?")
    public void createAutomaticBackup() {
        log.info("Starting automatic backup...");
        
        try {
            // Create backup
            BackupInfoDto backupInfo = backupService.createBackup(
                BackupType.AUTOMATIC, 
                "Automatic backup"
            );
            
            log.info("Automatic backup created: {}", backupInfo.getBackupName());
            
            // Upload to Google Drive if enabled
            if (googleDriveService.isConfigured()) {
                Path zipPath = Paths.get(backupInfo.getLocalPath());
                String driveFileId = googleDriveService.uploadToGoogleDrive(
                    zipPath, 
                    backupInfo.getBackupName() + ".zip"
                );
                
                if (driveFileId != null) {
                    // Update metadata with Google Drive file ID
                    backupMetadataRepo.findById(backupInfo.getId()).ifPresent(meta -> {
                        meta.setGoogleDriveFileId(driveFileId);
                        backupMetadataRepo.save(meta);
                    });
                    
                    log.info("Backup uploaded to Google Drive: {}", driveFileId);
                } else {
                    log.warn("Failed to upload backup to Google Drive");
                }
            } else {
                log.info("Google Drive upload skipped (not configured)");
            }
            
            log.info("Automatic backup completed successfully");
            
        } catch (Exception e) {
            log.error("Failed to create automatic backup: {}", e.getMessage(), e);
        }
    }
}

