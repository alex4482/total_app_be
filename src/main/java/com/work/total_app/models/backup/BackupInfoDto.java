package com.work.total_app.models.backup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning backup information to the client.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BackupInfoDto {
    private Long id;
    private String backupName;
    private String backupType; // MANUAL / AUTOMATIC
    private String createdAt; // ISO-8601 format
    private String localPath;
    private String googleDriveFileId;
    private Long sizeBytes;
    private String description;
}

