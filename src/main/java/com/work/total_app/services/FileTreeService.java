package com.work.total_app.services;

import com.work.total_app.models.file.FileTreeNodeDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for building file tree structures from the storage directory.
 */
@Service
@Log4j2
public class FileTreeService {

    @Value("${app.storage.baseDir:/FISIERE}")
    private String storageBaseDir;

    /**
     * Get the complete file tree structure starting from the storage base directory.
     * 
     * @return Root node containing the entire file tree
     * @throws IOException if there's an error reading the file system
     */
    public FileTreeNodeDto getFileTree() throws IOException {
        Path rootPath = Paths.get(storageBaseDir);
        
        if (!Files.exists(rootPath)) {
            log.warn("Storage directory does not exist: {}", storageBaseDir);
            Files.createDirectories(rootPath);
        }
        
        return buildTreeNode(rootPath, "");
    }

    /**
     * Get file tree for a specific subdirectory.
     * 
     * @param relativePath Path relative to storage base directory
     * @return Tree node for the specified path
     * @throws IOException if there's an error reading the file system
     */
    public FileTreeNodeDto getFileTree(String relativePath) throws IOException {
        Path rootPath = Paths.get(storageBaseDir);
        Path targetPath = rootPath.resolve(relativePath).normalize();
        
        // Security check: ensure the target path is within storage directory
        if (!targetPath.startsWith(rootPath)) {
            throw new SecurityException("Access denied: path is outside storage directory");
        }
        
        if (!Files.exists(targetPath)) {
            throw new IOException("Path does not exist: " + relativePath);
        }
        
        return buildTreeNode(targetPath, relativePath);
    }

    /**
     * Get flat list of all files (no folders) in the storage directory.
     * 
     * @return List of file nodes (no tree structure)
     * @throws IOException if there's an error reading the file system
     */
    public List<FileTreeNodeDto> getAllFiles() throws IOException {
        Path rootPath = Paths.get(storageBaseDir);
        
        if (!Files.exists(rootPath)) {
            return new ArrayList<>();
        }
        
        List<FileTreeNodeDto> files = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     try {
                         String relativePath = rootPath.relativize(path).toString().replace("\\", "/");
                         files.add(createFileNode(path, relativePath));
                     } catch (IOException e) {
                         log.error("Error reading file: {}", path, e);
                     }
                 });
        }
        
        // Sort by path
        files.sort(Comparator.comparing(FileTreeNodeDto::getPath));
        
        return files;
    }

    /**
     * Recursively build a tree node for a given path.
     */
    private FileTreeNodeDto buildTreeNode(Path path, String relativePath) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        
        FileTreeNodeDto node = new FileTreeNodeDto();
        node.setName(path.getFileName() != null ? path.getFileName().toString() : storageBaseDir);
        node.setPath(relativePath.isEmpty() ? "/" : "/" + relativePath.replace("\\", "/"));
        node.setLastModified(attrs.lastModifiedTime().toInstant());
        
        if (attrs.isDirectory()) {
            node.setType("folder");
            node.setChildren(new ArrayList<>());
            
            // Read children
            try (Stream<Path> children = Files.list(path)) {
                children.sorted((a, b) -> {
                    // Folders first, then files, alphabetically
                    boolean aIsDir = Files.isDirectory(a);
                    boolean bIsDir = Files.isDirectory(b);
                    if (aIsDir && !bIsDir) return -1;
                    if (!aIsDir && bIsDir) return 1;
                    return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                }).forEach(childPath -> {
                    try {
                        String childRelativePath = relativePath.isEmpty() 
                            ? childPath.getFileName().toString()
                            : relativePath + "/" + childPath.getFileName().toString();
                        FileTreeNodeDto childNode = buildTreeNode(childPath, childRelativePath);
                        node.getChildren().add(childNode);
                    } catch (IOException e) {
                        log.error("Error reading child path: {}", childPath, e);
                    }
                });
            }
            
            node.setItemCount(node.getChildren().size());
            
        } else {
            node.setType("file");
            node.setSize(attrs.size());
            node.setChildren(new ArrayList<>());
            
            // Extract file extension
            String fileName = node.getName();
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0 && lastDot < fileName.length() - 1) {
                node.setExtension(fileName.substring(lastDot + 1).toLowerCase());
            }
            
            // Try to determine MIME type
            try {
                String mimeType = Files.probeContentType(path);
                node.setMimeType(mimeType);
            } catch (IOException e) {
                log.debug("Could not determine MIME type for: {}", path);
            }
        }
        
        return node;
    }

    /**
     * Create a simple file node without recursion.
     */
    private FileTreeNodeDto createFileNode(Path path, String relativePath) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        
        FileTreeNodeDto node = new FileTreeNodeDto();
        node.setName(path.getFileName().toString());
        node.setPath("/" + relativePath.replace("\\", "/"));
        node.setType("file");
        node.setSize(attrs.size());
        node.setLastModified(attrs.lastModifiedTime().toInstant());
        node.setChildren(new ArrayList<>());
        
        // Extract file extension
        String fileName = node.getName();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            node.setExtension(fileName.substring(lastDot + 1).toLowerCase());
        }
        
        // Try to determine MIME type
        try {
            String mimeType = Files.probeContentType(path);
            node.setMimeType(mimeType);
        } catch (IOException e) {
            log.debug("Could not determine MIME type for: {}", path);
        }
        
        return node;
    }

    /**
     * Get storage statistics.
     */
    public StorageStatsDto getStorageStats() throws IOException {
        Path rootPath = Paths.get(storageBaseDir);
        
        if (!Files.exists(rootPath)) {
            return new StorageStatsDto(0L, 0, 0);
        }
        
        long totalSize = 0;
        int fileCount = 0;
        int folderCount = 0;
        
        try (Stream<Path> paths = Files.walk(rootPath)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (Files.isRegularFile(path)) {
                    totalSize += Files.size(path);
                    fileCount++;
                } else if (Files.isDirectory(path) && !path.equals(rootPath)) {
                    folderCount++;
                }
            }
        }
        
        return new StorageStatsDto(totalSize, fileCount, folderCount);
    }

    /**
     * Simple DTO for storage statistics.
     */
    public static class StorageStatsDto {
        public final long totalSizeBytes;
        public final int fileCount;
        public final int folderCount;

        public StorageStatsDto(long totalSizeBytes, int fileCount, int folderCount) {
            this.totalSizeBytes = totalSizeBytes;
            this.fileCount = fileCount;
            this.folderCount = folderCount;
        }
    }
}

