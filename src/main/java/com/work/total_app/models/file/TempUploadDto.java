package com.work.total_app.models.file;

import java.util.UUID;

/**
 * Metadata describing a file staged in temporary storage.
 * Returned by the temp upload endpoint for client-side correlation.
 *
 * Fields:
 *  - tempId: identifier of the TempUpload row
 *  - batchId: grouping id for multi-file uploads
 *  - filename: original filename
 *  - contentType: media type if provided by client/UA
 *  - sizeBytes: byte length of the uploaded file
 */
public record TempUploadDto(
        UUID tempId,
        UUID batchId,
        String filename,
        String contentType,
        long sizeBytes
) {}
