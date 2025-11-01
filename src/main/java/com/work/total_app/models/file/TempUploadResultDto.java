package com.work.total_app.models.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of uploading multiple files to temporary storage, with detailed success/failure information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TempUploadResultDto {
    
    /**
     * Total number of files in the upload request
     */
    private int totalFiles;
    
    /**
     * Number of files successfully uploaded
     */
    private int successCount;
    
    /**
     * Number of files that failed to upload
     */
    private int failedCount;
    
    /**
     * Batch ID for this upload session
     */
    private String batchId;
    
    /**
     * List of successfully uploaded files
     */
    private List<TempUploadDto> successfulFiles = new ArrayList<>();
    
    /**
     * List of files that failed to upload with reasons
     */
    private List<FailedUpload> failedFiles = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedUpload {
        private String filename;
        private String reason;
    }
}

