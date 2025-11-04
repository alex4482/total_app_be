package com.work.total_app.models.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a file or folder in a tree structure.
 * Used for displaying file system hierarchy in the frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileTreeNodeDto {
    
    /**
     * Name of the file or folder
     */
    private String name;
    
    /**
     * Full path relative to storage base directory
     */
    private String path;
    
    /**
     * Type: "file" or "folder"
     */
    private String type;
    
    /**
     * File size in bytes (null for folders)
     */
    private Long size;
    
    /**
     * Last modified timestamp
     */
    private Instant lastModified;
    
    /**
     * File extension (null for folders)
     */
    private String extension;
    
    /**
     * MIME type (null for folders)
     */
    private String mimeType;
    
    /**
     * Children nodes (empty for files)
     */
    private List<FileTreeNodeDto> children = new ArrayList<>();
    
    /**
     * Whether this node is expanded in the UI (optional, for frontend state)
     */
    private Boolean expanded;
    
    /**
     * Number of items in folder (null for files)
     */
    private Integer itemCount;
}

