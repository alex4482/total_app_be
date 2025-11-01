package com.work.total_app.controllers;

import com.work.total_app.helpers.files.DatabaseHelper;
import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.file.FileCommitResultDto;
import com.work.total_app.models.file.FileDto;
import com.work.total_app.models.file.OwnerType;
import com.work.total_app.models.file.OwnerRef;
import com.work.total_app.services.FileCommitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * REST controller for querying file metadata by owner tuple (type + id).
 *
 * Base path: /files
 *
 * Endpoints:
 *  - GET /files?ownerType=...&ownerId=...
 *    Returns a list of FileDto with metadata and a download link for each file.
 *  - POST /files/commit
 *    Commits temporary files to a specific owner (TENANT, BUILDING, etc.)
 */
@RestController
@RequestMapping("/files")
public class FileQueryController {

    @Autowired
    private DatabaseHelper databaseHelper;

    @Autowired
    private FileCommitService fileCommitService;

    /**
     * List files by owner type and owner id.
     * Returns 400 if ownerType is invalid.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<FileDto>>> listByOwner(
            @RequestParam("ownerType") String ownerType,
            @RequestParam("ownerId") Long ownerId
    ) {
        if (ownerType == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Owner type is required"));
        }
        final OwnerType type;
        try {
            type = OwnerType.valueOf(ownerType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid owner type: " + ownerType));
        }

        var list = databaseHelper.listByOwner(type, ownerId).stream()
                .map(f -> new FileDto(
                        f.getId().toString(),
                        f.getOwnerType().name(),
                        f.getOwnerId(),
                        f.getOriginalFilename(),
                        f.getContentType(),
                        f.getSizeBytes(),
                        f.getChecksum(),
                        "/files/" + f.getId(),
                        f.getModifiedAt() != null ? f.getModifiedAt().toString() : null,
                        f.getUploadedAt() != null ? f.getUploadedAt().toString() : null
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * Commit temporary uploaded files to a specific owner.
     *
     * @param ownerType - Type of owner (TENANT, BUILDING, ROOM, etc.)
     * @param ownerId - ID of the owner entity
     * @param tempIds - List of temporary file IDs from /uploads/temp
     * @param overwrite - If true, allows overwriting existing files with same name
     * @return List of committed files with permanent IDs and download URLs
     */
    @PostMapping("/commit")
    public ResponseEntity<ApiResponse<FileCommitResultDto>> commitFiles(
            @RequestParam("ownerType") String ownerType,
            @RequestParam("ownerId") Long ownerId,
            @RequestParam("tempIds") List<UUID> tempIds,
            @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite
    ) {
        if (ownerType == null || ownerId == null || tempIds == null || tempIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Missing required parameters: ownerType, ownerId, and tempIds"));
        }

        final OwnerType type;
        try {
            type = OwnerType.valueOf(ownerType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid ownerType. Valid values: " + String.join(", ",
                            java.util.Arrays.stream(OwnerType.values())
                                    .map(Enum::name)
                                    .toArray(String[]::new))));
        }

        OwnerRef owner = new OwnerRef(type, ownerId);
        FileCommitResultDto result = fileCommitService.commitBulk(owner, tempIds, overwrite);
        
        // Determine success message based on results
        String message;
        if (result.getFailedCount() == 0) {
            message = String.format("All %d files committed successfully", result.getSuccessCount());
        } else if (result.getSuccessCount() == 0) {
            message = String.format("All %d files failed to commit", result.getFailedCount());
        } else {
            message = String.format("Committed %d out of %d files successfully", 
                    result.getSuccessCount(), result.getTotalFiles());
        }
        
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}
