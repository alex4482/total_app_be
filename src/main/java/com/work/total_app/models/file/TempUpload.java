package com.work.total_app.models.file;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "temp_upload")
@Data
public class TempUpload {

    @Id
    @Column(name="id")
    private UUID id;

    @Column(name="batch_id", nullable=false)
    private UUID batchId;

    @Column(name="original_filename", nullable=false)
    private String originalFilename;

    @Column(name="content_type")
    private String contentType;

    @Column(name="size_bytes")
    private long sizeBytes;

    @Column(name="checksum", length=64)
    private String checksum;

    @Column(name="temp_path", nullable=false)
    private String tempPath;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Column(name="expires_at", nullable=false)
    private Instant expiresAt;

    public TempUpload() {}

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (expiresAt == null) expiresAt = createdAt.plus(Duration.ofHours(6));
    }
}
