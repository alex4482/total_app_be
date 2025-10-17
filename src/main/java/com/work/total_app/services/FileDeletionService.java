package com.work.total_app.services;

import com.work.total_app.helpers.files.DatabaseHelper;
import com.work.total_app.helpers.files.FileSystemHelper;
import com.work.total_app.models.file.FileAsset;
import com.work.total_app.models.file.OwnerRef;
import com.work.total_app.models.file.OwnerType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for deleting files and their associated filesystem data.
 * Handles both database cleanup and filesystem deletion.
 */
@Service
@Log4j2
public class FileDeletionService {

    @Autowired
    private DatabaseHelper databaseHelper;

    @Autowired
    @Lazy
    private FileSystemHelper fileSystemHelper;

    /**
     * Delete all files belonging to a specific owner.
     * This includes both database records and filesystem files.
     *
     * @param ownerType - Type of owner (TENANT, BUILDING, etc.)
     * @param ownerId - ID of the owner
     */
    @Transactional
    public void deleteAllFilesForOwner(OwnerType ownerType, Long ownerId) {
        log.info("Deleting all files for owner: {} with id: {}", ownerType, ownerId);

        // First, get all files to delete from filesystem
        List<FileAsset> files = databaseHelper.listByOwner(ownerType, ownerId);

        // Delete from database first (transactional)
        databaseHelper.deleteByOwner(ownerType, ownerId);

        // Then delete from filesystem (best effort - won't rollback transaction)
        OwnerRef owner = new OwnerRef(ownerType, ownerId);
        for (FileAsset file : files) {
            try {
                Path filePath = fileSystemHelper.buildPermanentPath(owner, file.getId(), file.getOriginalFilename());
                fileSystemHelper.delete(filePath);
                log.debug("Deleted file from filesystem: {}", filePath);
            } catch (Exception e) {
                log.error("Failed to delete file from filesystem: {} - {}", file.getOriginalFilename(), e.getMessage());
                // Continue with other files even if one fails
            }
        }

        log.info("Successfully deleted {} files for owner: {} with id: {}", files.size(), ownerType, ownerId);
    }

    /**
     * Delete all files for a TENANT specifically.
     * Convenience method for tenant deletion.
     */
    public void deleteAllFilesForTenant(Long tenantId) {
        deleteAllFilesForOwner(OwnerType.TENANT, tenantId);
    }
}
