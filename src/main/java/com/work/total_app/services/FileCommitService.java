package com.work.total_app.services;

import com.work.total_app.helpers.files.DatabaseHelper;
import com.work.total_app.helpers.files.FileSystemHelper;
import com.work.total_app.models.file.FileAsset;
import com.work.total_app.models.file.FileDto;
import com.work.total_app.models.file.OwnerRef;
import com.work.total_app.models.file.TempUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class FileCommitService {

    @Autowired
    private DatabaseHelper databaseHelper;
    @Autowired
    private FileSystemHelper fileSystemHelper;

    /**
     * Commit selected temp uploads to a specific owner.
     * Features:
     *  - Enforces name uniqueness unless overwrite=true
     *  - Deduplicates by checksum per owner (reuses existing asset; deletes temp)
     *  - Uses afterCommit hook to promote temp -> final atomically; deletes temp on rollback
     */
    @Transactional
    public List<FileDto> commit(OwnerRef owner, List<UUID> tempIds, boolean overwrite) throws Exception {
        List<FileDto> result = new ArrayList<>();
        final List<Runnable> afterCommitMoves = new ArrayList<>();
        final List<Runnable> afterRollbackDeletes = new ArrayList<>();

        for (UUID tempId : tempIds) {
            TempUpload tu = databaseHelper.findTempById(tempId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid tempId: " + tempId));

            // 1) Dedup by checksum
            if (databaseHelper.existsByOwnerAndChecksum(owner.type(), owner.id(), tu.getChecksum())) {
                // schedule temp delete on rollback (in case tx rolls back)
                afterRollbackDeletes.add(() -> {
                    try { Files.deleteIfExists(Path.of(tu.getTempPath())); } catch (Exception ignored) {}
                });
                // remove temp row now
                databaseHelper.deleteTemp(tempId);
                // return existing file metadata
                var existing = databaseHelper.findByOwnerAndChecksum(owner.type(), owner.id(), tu.getChecksum()).orElseThrow();
                result.add(new FileDto(
                        existing.getId().toString(),
                        existing.getOwnerType().name(),
                        existing.getOwnerId(),
                        existing.getOriginalFilename(),
                        existing.getContentType(),
                        existing.getSizeBytes(),
                        existing.getChecksum(),
                        "/files/" + existing.getId(),
                        existing.getModifiedAt() != null ? existing.getModifiedAt().toString() : null,
                        existing.getUploadedAt() != null ? existing.getUploadedAt().toString() : null
                ));
                continue;
            }

            // 2) Enforce name uniqueness if not overwriting
            if (!overwrite && databaseHelper.existsByOwnerAndName(owner.type(), owner.id(), tu.getOriginalFilename())) {
                throw new IllegalStateException("Filename already exists for owner: " + tu.getOriginalFilename());
            }

            // 3) Compute final path now; move happens after commit
            UUID finalId = UUID.randomUUID();
            Path finalPath = fileSystemHelper.buildPermanentPath(owner, finalId, tu.getOriginalFilename());
            Path tempPath = Path.of(tu.getTempPath());

            // 4) Read bytes from TEMP (for BLOB) BEFORE commit completes
            byte[] data = Files.readAllBytes(tempPath);

            // 5) Persist DB row
            FileAsset fa = new FileAsset();
            fa.setId(finalId);
            fa.setOwnerType(owner.type());
            fa.setOwnerId(owner.id());
            fa.setOriginalFilename(tu.getOriginalFilename());
            fa.setContentType(tu.getContentType());
            fa.setSizeBytes(tu.getSizeBytes());
            fa.setChecksum(tu.getChecksum());
            fa.setData(data); // set to null if you don't want BLOBs in DB
            // uploadedAt is set automatically by @PrePersist
            databaseHelper.save(fa);

            // 6) Schedule FS actions based on transaction outcome
            afterCommitMoves.add(() -> {
                try { fileSystemHelper.promoteTempToFinal(new FileSystemHelper.TempHandle(tempPath), finalPath); }
                catch (Exception e) { e.printStackTrace(); }
            });
            afterRollbackDeletes.add(() -> {
                try { Files.deleteIfExists(tempPath); } catch (Exception ignored) {}
            });

            // 7) Remove temp row now (file stays on disk as TEMP until afterCommit)
            databaseHelper.deleteTemp(tempId);

            result.add(new FileDto(
                    fa.getId().toString(),
                    fa.getOwnerType().name(),
                    fa.getOwnerId(),
                    fa.getOriginalFilename(),
                    fa.getContentType(),
                    fa.getSizeBytes(),
                    fa.getChecksum(),
                    "/api/files/" + fa.getId(),
                    fa.getModifiedAt() != null ? fa.getModifiedAt().toString() : null,
                    fa.getUploadedAt() != null ? fa.getUploadedAt().toString() : null
            ));
        }

        // Register a single synchronization per commit call
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                for (var r : afterCommitMoves) r.run();
            }
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    for (var r : afterRollbackDeletes) r.run();
                }
            }
        });

        return result;
    }
}
