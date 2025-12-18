package com.work.total_app.services;

import com.work.total_app.helpers.files.DatabaseHelper;
import com.work.total_app.helpers.files.FileSystemHelper;
import com.work.total_app.models.file.*;
import com.work.total_app.validators.FileUploadValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import lombok.extern.log4j.Log4j2;

/**
 * Coordinates file storage across DB and filesystem.
 *
 * Responsibilities:
 *  - Stage incoming multipart files in a temporary area (filesystem + DB row in TempUpload)
 *  - Commit selected temp uploads to a permanent FileAsset tied to an owner (OwnerRef)
 *  - Enforce filename uniqueness per owner (unless overwrite=true)
 *  - Deduplicate by checksum per owner to avoid duplicate storage
 *  - Use transaction synchronization to move files on the filesystem only after DB commit
 */
@Service
@Log4j2
public class FileStorageService {

    @Autowired
    private DatabaseHelper databaseHelper;
    @Autowired
    private FileSystemHelper fileSystemHelper;
    @Autowired
    private FileUploadValidator fileUploadValidator;

    /**
     * Stage a batch of files in temporary storage associated with a batchId.
     */
    @Transactional
    public List<TempUploadDto> uploadTempBatch(UUID batchId, List<MultipartFile> files) throws Exception {
        List<TempUploadDto> out = new ArrayList<>();
        for (MultipartFile mf : files) {
            // SECURITY: Validate file upload before processing
            fileUploadValidator.validate(mf);
            
            // SECURITY: Sanitize filename to prevent path traversal and injection attacks
            String sanitizedFilename = fileUploadValidator.sanitizeFilename(mf.getOriginalFilename());
            
            byte[] data = mf.getBytes();
            String checksum = sha256(data);
            
            // Write to temp folder on disk (using sanitized filename)
            FileSystemHelper.TempHandle handle = fileSystemHelper.writeTemp(batchId, sanitizedFilename, data);

            // Persist a TempUpload row pointing to temp path
            TempUpload tu = new TempUpload();
            tu.setBatchId(batchId);
            tu.setOriginalFilename(sanitizedFilename);
            tu.setContentType(mf.getContentType());
            tu.setSizeBytes(data.length);
            tu.setChecksum(checksum);
            tu.setTempPath(handle.tempPath().toString());
            databaseHelper.saveTemp(tu);

            log.info("File uploaded successfully: {} ({} bytes, checksum: {})", 
                    sanitizedFilename, data.length, checksum);

            out.add(new TempUploadDto(tu.getId(), tu.getBatchId(), tu.getOriginalFilename(),
                            tu.getContentType(), tu.getSizeBytes()));
        }
        return out;
    }

    /**
     * Commit selected temp uploads to a specific owner.
     * Features:
     *  - Enforces name uniqueness unless overwrite=true
     *  - Deduplicates by checksum per owner (skips creating a new asset if same checksum exists; deletes temp)
     *  - Uses afterCommit hook to promote temp -> final atomically; deletes temp on rollback
     */
    @Transactional
    public List<FileDto> commit(OwnerRef owner, List<UUID> tempIds, boolean overwrite) throws Exception {
        List<FileDto> result = new ArrayList<>();
        final List<Runnable> afterCommitMoves = new ArrayList<>();
        final List<Runnable> afterRollbackDeletes = new ArrayList<>();

        for (UUID tempId : tempIds) {
            TempUpload tu = databaseHelper.findTempById(tempId).orElseThrow(() ->
                    new IllegalArgumentException("Invalid tempId: " + tempId));

            // 1) Dedup by checksum
            if (databaseHelper.existsByOwnerAndChecksum(owner.type(), owner.id(), tu.getChecksum())) {
                // schedule temp deletion if transaction rolls back
                afterRollbackDeletes.add(() -> {
                    try { Files.deleteIfExists(Path.of(tu.getTempPath())); } catch (Exception ignored) {}
                });
                // temp row cleanup inside transaction
                databaseHelper.deleteTemp(tempId);
                // also return the existing asset info (no duplicate created)
                FileAsset existing = databaseHelper
                        .findByOwnerAndChecksum(owner.type(), owner.id(), tu.getChecksum())
                        .orElseThrow();
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
                        existing.getUploadedAt() != null ? existing.getUploadedAt().toString() : null,
                        true // Deduplicated file
                ));
                continue;
            }

            // 2) Name uniqueness (unless overwrite)
            if (!overwrite && databaseHelper.existsByOwnerAndName(owner.type(), owner.id(), tu.getOriginalFilename())) {
                throw new IllegalStateException("Filename already exists for owner: " + tu.getOriginalFilename());
            }

            // 3) Compute final path now; move happens after commit
            UUID finalId = UUID.randomUUID();
            Path finalPath = fileSystemHelper.buildPermanentPath(owner, finalId, tu.getOriginalFilename());
            Path tempPath = Path.of(tu.getTempPath());

            // 4) Persist DB row (metadata only - file content is on filesystem)
            FileAsset fa = new FileAsset();
            fa.setId(finalId);
            fa.setOwnerType(owner.type());
            fa.setOwnerId(owner.id());
            fa.setOriginalFilename(tu.getOriginalFilename());
            fa.setContentType(tu.getContentType());
            fa.setSizeBytes(tu.getSizeBytes());
            fa.setChecksum(tu.getChecksum());
            // uploadedAt is set automatically by @PrePersist
            databaseHelper.save(fa);

            // 6) schedule FS actions: move temp -> final after successful commit
            afterCommitMoves.add(() -> {
                try { fileSystemHelper.promoteTempToFinal(new FileSystemHelper.TempHandle(tempPath), finalPath); }
                catch (Exception e) { e.printStackTrace(); }
            });
            afterRollbackDeletes.add(() -> {
                try { Files.deleteIfExists(tempPath); } catch (Exception ignored) {}
            });

            // 7) cleanup temp row now
            databaseHelper.deleteTemp(tempId);

            result.add(new FileDto(
                    fa.getId().toString(),
                    fa.getOwnerType().name(),
                    fa.getOwnerId(),
                    fa.getOriginalFilename(),
                    fa.getContentType(),
                    fa.getSizeBytes(),
                    fa.getChecksum(),
                    "/files/" + fa.getId(),
                    fa.getModifiedAt() != null ? fa.getModifiedAt().toString() : null,
                    fa.getUploadedAt() != null ? fa.getUploadedAt().toString() : null,
                    false // New file, not a duplicate
            ));
        }

        // Register synchronization once so filesystem changes follow transaction outcome
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

    /**
     * Upload multiple files to temporary storage with detailed result tracking.
     * Each file is uploaded in its own transaction, so failures don't affect other files.
     * 
     * @param batchId - Batch ID for grouping files
     * @param files - List of files to upload
     * @return Detailed result with success/failure for each file
     */
    public TempUploadResultDto uploadTempBatchBulk(UUID batchId, List<MultipartFile> files) {
        // Generate batch ID if not provided
        UUID actualBatchId = batchId != null ? batchId : UUID.randomUUID();
        
        TempUploadResultDto result = new TempUploadResultDto();
        result.setTotalFiles(files.size());
        result.setBatchId(actualBatchId.toString());
        
        for (MultipartFile mf : files) {
            String filename = mf.getOriginalFilename() != null ? mf.getOriginalFilename() : "unnamed";
            
            try {
                TempUploadDto dto = uploadTempSingle(actualBatchId, mf);
                result.getSuccessfulFiles().add(dto);
                result.setSuccessCount(result.getSuccessCount() + 1);
                
            } catch (Exception e) {
                log.error("Failed to upload file {}: {}", filename, e.getMessage(), e);
                TempUploadResultDto.FailedUpload failed = new TempUploadResultDto.FailedUpload();
                failed.setFilename(filename);
                failed.setReason(e.getMessage());
                result.getFailedFiles().add(failed);
                result.setFailedCount(result.getFailedCount() + 1);
            }
        }
        
        return result;
    }
    
    /**
     * Upload a single file to temporary storage in its own transaction.
     */
    @Transactional
    private TempUploadDto uploadTempSingle(UUID batchId, MultipartFile mf) throws Exception {
        // SECURITY: Validate file upload before processing
        fileUploadValidator.validate(mf);
        
        // SECURITY: Sanitize filename to prevent path traversal and injection attacks
        String sanitizedFilename = fileUploadValidator.sanitizeFilename(mf.getOriginalFilename());
        
        byte[] data = mf.getBytes();
        String checksum = sha256(data);
        
        // Write to temp folder on disk (using sanitized filename)
        FileSystemHelper.TempHandle handle = fileSystemHelper.writeTemp(batchId, sanitizedFilename, data);

        // Persist a TempUpload row pointing to temp path
        TempUpload tu = new TempUpload();
        tu.setBatchId(batchId);
        tu.setOriginalFilename(sanitizedFilename);
        tu.setContentType(mf.getContentType());
        tu.setSizeBytes(data.length);
        tu.setChecksum(checksum);
        tu.setTempPath(handle.tempPath().toString());
        databaseHelper.saveTemp(tu);

        log.info("File uploaded successfully: {} ({} bytes, checksum: {})", 
                sanitizedFilename, data.length, checksum);

        return new TempUploadDto(tu.getId(), tu.getBatchId(), tu.getOriginalFilename(),
                        tu.getContentType(), tu.getSizeBytes());
    }

    private static String sha256(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(bytes));
    }
}
