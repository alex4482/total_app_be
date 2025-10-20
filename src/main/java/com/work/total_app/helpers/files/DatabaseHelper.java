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

/**
 * Thin facade over Spring Data repositories for file storage concerns.
 * Exposes intent-revealing methods used by services/controllers without leaking repository details.
 */
@Component
public class DatabaseHelper {

    @Autowired
    private FileAssetRepository fileRepo;
    @Autowired
    private TempUploadRepository tempRepo;

    // --- Permanent assets ---

    /** Persist a new or updated FileAsset. */
    public FileAsset save(FileAsset asset) { return fileRepo.save(asset); }

    /** Check if an owner already has a file with the given name. */
    public boolean existsByOwnerAndName(OwnerType type, Long ownerId, String name) {
        return fileRepo.existsByOwnerTypeAndOwnerIdAndOriginalFilename(type, ownerId, name);
    }

    /** Check if an owner already has a file with the given checksum. */
    public boolean existsByOwnerAndChecksum(OwnerType type, Long ownerId, String checksum) {
        return fileRepo.existsByOwnerTypeAndOwnerIdAndChecksum(type, ownerId, checksum);
    }

    /** Find an asset for owner by checksum (used for deduplication). */
    public Optional<FileAsset> findByOwnerAndChecksum(OwnerType type, Long ownerId, String checksum) {
        return fileRepo.findByOwnerTypeAndOwnerIdAndChecksum(type, ownerId, checksum);
    }

    /** Find an asset by its ID. */
    public Optional<FileAsset> findById(UUID id) { return fileRepo.findById(id); }

    /** Delete a file asset by its ID. */
    public void deleteById(UUID id) { fileRepo.deleteById(id); }

    /** List all assets belonging to a given owner. */
    public List<FileAsset> listByOwner(OwnerType type, Long ownerId) {
        return fileRepo.findAllByOwnerTypeAndOwnerId(type, ownerId);
    }

    /** Delete all assets belonging to a given owner. */
    public void deleteByOwner(OwnerType type, Long ownerId) {
        fileRepo.deleteAllByOwnerTypeAndOwnerId(type, ownerId);
    }

    // --- Temporary uploads ---

    /** Persist a TempUpload row representing a staged file on disk. */
    public TempUpload saveTemp(TempUpload tu) { return tempRepo.save(tu); }

    /** Find a TempUpload by ID. */
    public Optional<TempUpload> findTempById(UUID id) { return tempRepo.findById(id); }

    /** List all TempUpload entries within the same batch. */
    public List<TempUpload> findTempByBatch(UUID batchId) { return tempRepo.findAllByBatchId(batchId); }

    /** Delete a TempUpload by ID. */
    public void deleteTemp(UUID id) { tempRepo.deleteById(id); }

    /** Find TempUpload rows that expired before the given timestamp (for cleanup jobs). */
    public List<TempUpload> findExpiredTemp(Instant now) { return tempRepo.findAllByExpiresAtBefore(now); }
}
