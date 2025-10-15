package com.work.total_app.helpers.files;

import com.work.total_app.models.file.OwnerRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.Optional;
import java.util.UUID;

@Component
public class FileSystemHelper {

    @Autowired
    private PathResolver pathResolver;

    public record TempHandle(Path tempPath) {}

    public TempHandle writeTemp(UUID batchId, String originalName, byte[] data) throws IOException {
        Path dir = Paths.get("/DATA/.TEMP", batchId.toString());
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, sanitize(stripExtension(originalName)) + "-", ".part");
        Files.write(tmp, data, StandardOpenOption.TRUNCATE_EXISTING);
        return new TempHandle(tmp);
    }

    public void promoteTempToFinal(TempHandle handle, Path finalPath) throws IOException {
        Files.createDirectories(finalPath.getParent());
        Files.move(handle.tempPath(), finalPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    public Path buildPermanentPath(OwnerRef owner, UUID finalId, String originalFilename) throws IOException {
        Path dir = pathResolver.resolvePermanentDir(owner);
        Files.createDirectories(dir);
        String ext = extensionOf(originalFilename).orElse("");
        String base = stripExtension(originalFilename);
        String safeBase = sanitize(base);
        String fileName = ext.isEmpty() ? finalId + "-" + safeBase : finalId + "-" + safeBase + "." + ext;
        return dir.resolve(fileName);
    }

    public boolean exists(Path p){ return Files.exists(p); }
    public byte[] read(Path p) throws IOException { return Files.readAllBytes(p); }
    public void delete(Path p) throws IOException { Files.deleteIfExists(p); }

    private static String stripExtension(String n){ int i=n.lastIndexOf('.'); return i>0?n.substring(0,i):n; }
    private static Optional<String> extensionOf(String n)
    { int i=n.lastIndexOf('.'); return (i>0 && i<n.length()-1) ? Optional.of(n.substring(i+1)) : Optional.empty(); }
    private static String sanitize(String s) {
        if (s==null || s.isBlank()) return "file";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t.replaceAll("[/:*?\"<>|]+","_").trim();
    }
}
