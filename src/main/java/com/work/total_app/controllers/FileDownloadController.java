package com.work.total_app.controllers;

import com.work.total_app.helpers.files.DatabaseHelper;
import com.work.total_app.helpers.files.FileSystemHelper;
import com.work.total_app.models.file.FileAsset;
import com.work.total_app.models.file.OwnerRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * REST controller for serving file downloads by file ID.
 *
 * Base path: /files
 * Endpoints:
 *  - GET /files/{id}
 *    Returns the raw bytes with appropriate Content-Type and Content-Disposition headers
 *    so the browser downloads the file using the original filename.
 *  - GET /files/download-zip?fileIds=...&fileIds=...
 *    Returns multiple files as a ZIP archive
 *
 * Storage note:
 *  - This implementation prefers the database BLOB (FileAsset.data). If your deployment stores
 *    only filesystem copies, you'll need to extend this controller to read from disk using
 *    FileSystemHelper (currently injected but not used in the happy path).
 */
@RestController
@RequestMapping("/files")
public class FileDownloadController {

    @Autowired
    private DatabaseHelper databaseHelper;
    @Autowired
    private FileSystemHelper fileSystemHelper; // reserved for FS fallback if BLOB is disabled

    /**
     * Download a file by its UUID.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadById(@PathVariable UUID id) {
        FileAsset f = databaseHelper.findById(id).orElse(null);
        if (f == null) {
            return ResponseEntity.notFound().build();
        }

        // Prepare headers
        String filename = f.getOriginalFilename();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        String contentType = f.getContentType() != null ? f.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        // Read file from filesystem (we don't store BLOBs in DB anymore)
        byte[] body;
        try {
            Path p = fileSystemHelper.buildPermanentPath(new OwnerRef(f.getOwnerType(), f.getOwnerId()), f.getId(), f.getOriginalFilename());
            if (!fileSystemHelper.exists(p)) {
                return ResponseEntity.notFound().build();
            }
            body = fileSystemHelper.read(p);
        } catch (Exception io) {
            // If FS access fails, expose as 404 to avoid leaking FS details
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentLength(f.getSizeBytes())
                .body(body);
    }

    /**
     * Download multiple files as a ZIP archive.
     *
     * @param fileIds - List of file UUIDs to include in the ZIP
     * @return ZIP file containing all requested files
     */
    @GetMapping("/download-zip")
    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadZip(@RequestParam("fileIds") List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Missing required parameter: fileIds");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);

            int filesAdded = 0;
            for (UUID fileId : fileIds) {
                FileAsset file = databaseHelper.findById(fileId).orElse(null);
                if (file == null) {
                    continue; // Skip missing files
                }

                // Read file from filesystem (we don't store BLOBs in DB anymore)
                byte[] fileData;
                try {
                    Path p = fileSystemHelper.buildPermanentPath(
                            new OwnerRef(file.getOwnerType(), file.getOwnerId()),
                            file.getId(),
                            file.getOriginalFilename()
                    );
                    if (!fileSystemHelper.exists(p)) {
                        continue; // Skip missing files
                    }
                    fileData = fileSystemHelper.read(p);
                } catch (Exception e) {
                    continue; // Skip this file if filesystem read fails
                }

                if (fileData == null || fileData.length == 0) {
                    continue; // Skip files without data
                }

                // Create unique filename to avoid duplicates in ZIP
                String filename = file.getOriginalFilename();
                ZipEntry zipEntry = new ZipEntry(filename);
                zipOut.putNextEntry(zipEntry);
                zipOut.write(fileData);
                zipOut.closeEntry();
                filesAdded++;
            }

            zipOut.close();

            if (filesAdded == 0) {
                return ResponseEntity.notFound().build();
            }

            byte[] zipBytes = baos.toByteArray();

            // Generate ZIP filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String zipFilename = "files_" + timestamp + ".zip";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                            URLEncoder.encode(zipFilename, StandardCharsets.UTF_8))
                    .contentLength(zipBytes.length)
                    .body(zipBytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to create ZIP archive: " + e.getMessage());
        }
    }

    /**
     * Delete a file by its UUID.
     * Removes both the database record and the filesystem file (if it exists).
     *
     * @param id - UUID of the file to delete
     * @return 204 No Content on success, 404 if file not found
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        FileAsset file = databaseHelper.findById(id).orElse(null);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Delete from filesystem (if it exists there)
            Path filePath = fileSystemHelper.buildPermanentPath(
                    new OwnerRef(file.getOwnerType(), file.getOwnerId()),
                    file.getId(),
                    file.getOriginalFilename()
            );
            fileSystemHelper.delete(filePath);
        } catch (Exception e) {
            // Log but don't fail if filesystem deletion fails
            // The DB deletion is more important for consistency
        }

        // Delete from database
        databaseHelper.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
