package com.work.total_app.helpers.files;

import com.work.total_app.config.StorageProperties;
import com.work.total_app.models.file.OwnerRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.Optional;
import java.util.UUID;

/**
 * Filesystem utilities for file storage:
 *  - Write incoming data to a temp location grouped by batchId
 *  - Build a deterministic final path for an owner + file id + filename
 *  - Promote a temp file to its final destination after DB commit
 *  - Provide basic read/exists/delete helpers
 */
@Component
public class FileSystemHelper {

    @Autowired
    private PathResolver pathResolver;
    
    @Autowired
    private StorageProperties storageProperties;

    /** Handle for a temp file written on disk. */
    public record TempHandle(Path tempPath) {}

    /**
     * Write the given data to a temporary folder for the provided batchId.
     * Uses a sanitized stem derived from the original name and a random suffix for uniqueness.
     */
    public TempHandle writeTemp(UUID batchId, String originalName, byte[] data) throws IOException {
        Path dir = Paths.get(storageProperties.getBaseDir(), ".TEMP", batchId.toString());
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, sanitize(stripExtension(originalName)) + "-", ".part");
        Files.write(tmp, data, StandardOpenOption.TRUNCATE_EXISTING);
        return new TempHandle(tmp);
    }

    /** Move the temp file to a final path (atomic when supported by the filesystem). */
    public void promoteTempToFinal(TempHandle handle, Path finalPath) throws IOException {
        Files.createDirectories(finalPath.getParent());
        Files.move(handle.tempPath(), finalPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    /** Build a safe, stable permanent path for an owner's file. */
    public Path buildPermanentPath(OwnerRef owner, UUID finalId, String originalFilename) throws IOException {
        Path dir = pathResolver.resolvePermanentDir(owner);
        Files.createDirectories(dir);
        String ext = extensionOf(originalFilename).orElse("");
        String base = stripExtension(originalFilename);
        String safeBase = sanitize(base);
        String fileName = ext.isEmpty() ? finalId + "-" + safeBase : finalId + "-" + safeBase + "." + ext;
        return dir.resolve(fileName);
    }

    // lightweight helpers
    public boolean exists(Path p){ return Files.exists(p); }
    public byte[] read(Path p) throws IOException { return Files.readAllBytes(p); }
    public void delete(Path p) throws IOException { Files.deleteIfExists(p); }

    // --- internal name helpers ---
    private static String stripExtension(String n){ int i=n.lastIndexOf('.'); return i>0?n.substring(0,i):n; }
    private static Optional<String> extensionOf(String n)
    { int i=n.lastIndexOf('.'); return (i>0 && i<n.length()-1) ? Optional.of(n.substring(i+1)) : Optional.empty(); }
    private static String sanitize(String s) {
        if (s==null || s.isBlank()) return "file";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t.replaceAll("[/:*?\"<>|]+","_").trim();
    }
}
