package com.work.total_app.models.backup;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Metadata about a backup stored in the database.
 * The actual backup files are stored in the filesystem and optionally in Google Drive.
 */
@Entity
@Table(name = "backup_metadata")
@Data
public class BackupMetadata {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String backupName; // e.g., "backup_2025-11-03_14-30-15_manual"
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupType backupType;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private String localPath; // Path to the backup folder/zip
    
    @Column
    private String googleDriveFileId; // Google Drive file ID (if uploaded)
    
    @Column
    private Long sizeBytes;
    
    @Column
    private String description;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

