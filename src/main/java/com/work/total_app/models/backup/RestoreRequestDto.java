package com.work.total_app.models.backup;

import lombok.Data;

/**
 * Request DTO for restoring a backup.
 */
@Data
public class RestoreRequestDto {
    private String backupName; // Name or ID of the backup to restore
    private Boolean fromExcel = false; // If true, restore from Excel files instead of JSON
}

