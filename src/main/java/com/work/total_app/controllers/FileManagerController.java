package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.file.FileTreeNodeDto;
import com.work.total_app.services.FileTreeService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for file manager operations.
 * Provides endpoints for browsing the file system structure.
 */
@RestController
@RequestMapping("/file-manager")
@Log4j2
public class FileManagerController {

    @Autowired
    private FileTreeService fileTreeService;

    /**
     * Get the file tree structure.
     * GET /file-manager/tree
     * GET /file-manager/tree?path=/some/folder
     * 
     * Returns a hierarchical tree structure of all files and folders.
     * If path is empty or not provided, returns the complete tree starting from the storage base directory.
     * If path is provided, returns the tree starting from that subdirectory.
     * 
     * @param path Relative path from storage base directory (optional)
     * @return File tree structure
     */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<FileTreeNodeDto>> getFileTree(
            @RequestParam(required = false, defaultValue = "") String path) {
        try {
            log.info("Fetching file tree for path: {}", path.isEmpty() ? "root" : path);
            
            FileTreeNodeDto tree = path.isEmpty() 
                ? fileTreeService.getFileTree()
                : fileTreeService.getFileTree(path);
                
            return ResponseEntity.ok(ApiResponse.success(tree));
        } catch (SecurityException e) {
            log.warn("Security violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching file tree for path '{}': {}", path, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch file tree: " + e.getMessage()));
        }
    }

    /**
     * Get flat list of all files (no folders, no tree structure).
     * GET /file-manager/files
     * 
     * Useful for searching or displaying a simple file list.
     * 
     * @return List of all files
     */
    @GetMapping("/files")
    public ResponseEntity<ApiResponse<List<FileTreeNodeDto>>> getAllFiles() {
        try {
            log.info("Fetching all files (flat list)");
            List<FileTreeNodeDto> files = fileTreeService.getAllFiles();
            return ResponseEntity.ok(ApiResponse.success(files));
        } catch (Exception e) {
            log.error("Error fetching all files: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch files: " + e.getMessage()));
        }
    }

    /**
     * Get storage statistics.
     * GET /file-manager/stats
     * 
     * Returns information about total storage usage, file count, and folder count.
     * 
     * @return Storage statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<FileTreeService.StorageStatsDto>> getStorageStats() {
        try {
            log.info("Fetching storage statistics");
            FileTreeService.StorageStatsDto stats = fileTreeService.getStorageStats();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Error fetching storage stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch storage stats: " + e.getMessage()));
        }
    }
}

