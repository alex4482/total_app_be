package com.work.total_app.controllers;

import com.work.total_app.models.file.TempUploadDto;
import com.work.total_app.services.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
    public List<TempUploadDto> uploadTemp(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value="batchId", required=false) UUID batchId
    ) throws Exception {
        // Generate a grouping ID if caller didn't send one (helps correlate multi-file uploads)
        if (batchId == null) batchId = UUID.randomUUID();
        // Delegate to service which writes to temp storage and records metadata
        return fileStorageService.uploadTempBatch(batchId, files);
    }
}
