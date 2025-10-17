package com.work.total_app.models.file;

/**
 * Lightweight file metadata returned by the query/commit APIs.
 *
 * Fields:
 *  - id: UUID of the stored file asset (as string)
 *  - ownerType: OwnerType name the file belongs to
 *  - ownerId: numeric owner identifier
 *  - filename: original filename supplied at upload time
 *  - contentType: media type if known (may be null)
 *  - sizeBytes: file size in bytes
 *  - checksum: SHA-256 checksum used for deduplication
 *  - downloadUrl: absolute/relative URL to fetch the bytes (GET)
 *  - modifiedAt: Data ultimei modificări a fișierului (File.lastModified) - ISO format
 *  - uploadedAt: Data la care fișierul a fost încărcat pe server - ISO format
 */
public record FileDto(
        String id,
        String ownerType,
        Long ownerId,
        String filename,
        String contentType,
        long sizeBytes,
        String checksum,
        String downloadUrl,
        String modifiedAt,
        String uploadedAt
) {}
