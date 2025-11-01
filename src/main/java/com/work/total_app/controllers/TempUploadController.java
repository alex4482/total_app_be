package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.file.TempUploadResultDto;
import com.work.total_app.services.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for receiving temporary file uploads.
 *
 * Base path:
 *  - /uploads
 *
 * Endpoints:
 *  - POST /uploads/temp (multipart/form-data)
 *    Parameters:
 *      - files: one or more files (form field name: "files")
 *      - batchId: optional UUID to group files within the same client-side upload session
 *    Returns: List of TempUploadDto entries describing staged uploads.
 *
 * Notes:
 *  - If batchId is not supplied, a random one is generated to correlate the files.
 *  - The actual persistence and filesystem handling is delegated to {@link FileStorageService#uploadTempBatch(UUID, List)}.
 */
@RestController
@RequestMapping("/files")
public class TempUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Accepts files for temporary storage. These can later be "committed" to an owner entity.
     */
    @PostMapping(path="/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TempUploadResultDto>> uploadTemp(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value="batchId", required=false) UUID batchId
    ) {
        TempUploadResultDto result = fileStorageService.uploadTempBatchBulk(batchId, files);
        
        // Determine success message based on results
        String message;
        if (result.getFailedCount() == 0) {
            message = String.format("All %d files uploaded successfully", result.getSuccessCount());
        } else if (result.getSuccessCount() == 0) {
            message = String.format("All %d files failed to upload", result.getFailedCount());
        } else {
            message = String.format("Uploaded %d out of %d files successfully", 
                    result.getSuccessCount(), result.getTotalFiles());
        }
        
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}
