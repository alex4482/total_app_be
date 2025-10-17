package com.work.total_app.repositories;

import com.work.total_app.models.file.TempUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for TempUpload staging records.
 * Provides helpers to fetch entries by batch and to select expired rows for cleanup jobs.
 */
public interface TempUploadRepository extends JpaRepository<TempUpload, UUID> {
    /** Return temp uploads that expired before the given timestamp. */
    List<TempUpload> findAllByExpiresAtBefore(Instant instant);
    /** Return all temp uploads within a specific batch. */
    List<TempUpload> findAllByBatchId(UUID batchId);
}
