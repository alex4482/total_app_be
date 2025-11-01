package com.work.total_app.models.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of committing multiple files, with detailed success/failure information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileCommitResultDto {
    
    /**
     * Total number of files in the commit request
     */
    private int totalFiles;
    
    /**
     * Number of files successfully committed
     */
    private int successCount;
    
    /**
     * Number of files that failed to commit
     */
    private int failedCount;
    
    /**
     * List of successfully committed files
     */
    private List<CommittedFile> successfulFiles = new ArrayList<>();
    
    /**
     * List of files that failed to commit with reasons
     */
    private List<FailedFile> failedFiles = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommittedFile {
        private String tempId;
        private String filename;
        private FileDto fileDto;
        private String reason; // e.g., "Committed successfully", "Reused existing file (deduplicated)"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedFile {
        private String tempId;
        private String filename;
        private String reason;
    }
}

