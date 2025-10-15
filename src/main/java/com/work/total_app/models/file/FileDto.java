package com.work.total_app.models.file;

public record FileDto(
        String id,
        String ownerType,
        String ownerId,
        String filename,
        String contentType,
        long sizeBytes,
        String checksum,
        String downloadUrl
) {}
