package com.work.total_app.models.file;

/**
 * Tuple that identifies the domain owner of a file (type + id).
 * Used as an input to commit files to their permanent location and for path resolution.
 */
public record OwnerRef(OwnerType type, Long id) {}
