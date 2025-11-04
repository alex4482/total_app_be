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
@RequestMapping("/api/file-manager")
@Log4j2
public class FileManagerController {

    @Autowired
    private FileTreeService fileTreeService;

    /**
     * Get the complete file tree structure.
     * GET /api/file-manager/tree
     * 
     * Returns a hierarchical tree structure of all files and folders
     * starting from the storage base directory.
     * 
     * @return Complete file tree
     */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<FileTreeNodeDto>> getFileTree() {
        try {
            log.info("Fetching complete file tree");
            FileTreeNodeDto tree = fileTreeService.getFileTree();
            return ResponseEntity.ok(ApiResponse.success(tree));
        } catch (Exception e) {
            log.error("Error fetching file tree: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch file tree: " + e.getMessage()));
        }
    }

    /**
     * Get file tree for a specific subdirectory.
     * GET /api/file-manager/tree?path=/some/folder
     * 
     * @param path Relative path from storage base directory
     * @return File tree for the specified path
     */
    @GetMapping("/tree/path")
    public ResponseEntity<ApiResponse<FileTreeNodeDto>> getFileTreeByPath(
            @RequestParam(required = false, defaultValue = "") String path) {
        try {
            log.info("Fetching file tree for path: {}", path);
            
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
     * GET /api/file-manager/files
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
     * GET /api/file-manager/stats
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

