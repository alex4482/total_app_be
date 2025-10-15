package com.work.total_app.models.file;

import java.util.UUID;

public record TempUploadDto(
        UUID tempId,
        UUID batchId,
        String filename,
        String contentType,
        long sizeBytes
) {}
