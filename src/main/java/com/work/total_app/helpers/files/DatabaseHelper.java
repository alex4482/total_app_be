package com.work.total_app.helpers.files;

import com.work.total_app.models.file.FileAsset;
import com.work.total_app.models.file.OwnerType;
import com.work.total_app.models.file.TempUpload;
import com.work.total_app.repositories.FileAssetRepository;
import com.work.total_app.repositories.TempUploadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DatabaseHelper {

    @Autowired
    private FileAssetRepository fileRepo;
    @Autowired
    private TempUploadRepository tempRepo;

    // Permanent
    public FileAsset save(FileAsset asset) { return fileRepo.save(asset); }
    public boolean existsByOwnerAndName(OwnerType type, Long ownerId, String name) {
        return fileRepo.existsByOwnerTypeAndOwnerIdAndOriginalFilename(type, ownerId, name);
    }
    public boolean existsByOwnerAndChecksum(OwnerType type, Long ownerId, String checksum) {
        return fileRepo.existsByOwnerTypeAndOwnerIdAndChecksum(type, ownerId, checksum);
    }
    public Optional<FileAsset> findByOwnerAndChecksum(OwnerType type, Long ownerId, String checksum) {
        return fileRepo.findByOwnerTypeAndOwnerIdAndChecksum(type, ownerId, checksum);
    }
    public Optional<FileAsset> findById(UUID id) { return fileRepo.findById(id); }

    public List<FileAsset> listByOwner(OwnerType type, Long ownerId) {
        return fileRepo.findAllByOwnerTypeAndOwnerId(type, ownerId);
    }

    // Temp
    public TempUpload saveTemp(TempUpload tu) { return tempRepo.save(tu); }
    public Optional<TempUpload> findTempById(UUID id) { return tempRepo.findById(id); }
    public List<TempUpload> findTempByBatch(UUID batchId) { return tempRepo.findAllByBatchId(batchId); }
    public void deleteTemp(UUID id) { tempRepo.deleteById(id); }
    public List<TempUpload> findExpiredTemp(Instant now) { return tempRepo.findAllByExpiresAtBefore(now); }
}
