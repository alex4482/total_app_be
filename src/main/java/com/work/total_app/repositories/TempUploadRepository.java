package com.work.total_app.repositories;

import com.work.total_app.models.file.TempUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TempUploadRepository extends JpaRepository<TempUpload, UUID> {
    List<TempUpload> findAllByExpiresAtBefore(Instant instant);
    List<TempUpload> findAllByBatchId(UUID batchId);
}
