package com.work.total_app.services;

import com.work.total_app.models.file.TempUpload;
import com.work.total_app.models.file.TempUploadDto;
import com.work.total_app.repositories.TempUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TempUploadService {

    @Autowired
    private TempUploadRepository repo;
    private final Path tempRoot = Paths.get("/data/app-temp");

    public List<TempUploadDto> saveTempBatch(UUID batchId, List<MultipartFile> files) throws Exception {
        Files.createDirectories(tempRoot.resolve(batchId.toString()));
        List<TempUploadDto> out = new ArrayList<>();

        for (var mf : files) {
            byte[] data = mf.getBytes();
            String checksum = sha256(data);
            String safe = sanitize(mf.getOriginalFilename());
            UUID tempId = UUID.randomUUID();

            Path dir = tempRoot.resolve(batchId.toString());
            Path tmp = Files.createTempFile(dir, tempId + "-", ".part");
            Files.write(tmp, data);
            Path finalTmp = dir.resolve(tempId + "-" + safe);
            Files.move(tmp, finalTmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            TempUpload tu = new TempUpload();
            tu.setId(tempId);
            tu.setBatchId(batchId);
            tu.setOriginalFilename(safe);
            tu.setContentType(mf.getContentType());
            tu.setSizeBytes(data.length);
            tu.setChecksum(checksum);
            tu.setTempPath(finalTmp.toString());
            repo.save(tu);

            out.add(new TempUploadDto(tempId, batchId, safe, mf.getContentType(), data.length));
        }
        return out;
    }

    public void deleteTemp(UUID tempId) throws IOException {
        var tu = repo.findById(tempId).orElse(null);
        if (tu == null) return;
        Files.deleteIfExists(Path.of(tu.getTempPath()));
        repo.deleteById(tempId);
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        return name.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
    }

    private static String sha256(byte[] b) throws Exception {
        var md = java.security.MessageDigest.getInstance("SHA-256");
        return java.util.HexFormat.of().formatHex(md.digest(b));
    }
}

