package com.work.total_app.models.file;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_asset",
       uniqueConstraints = {
         @UniqueConstraint(name="uq_owner_name", columnNames = {"owner_type","owner_id","original_filename"}),
         @UniqueConstraint(name="uq_owner_checksum", columnNames = {"owner_type","owner_id","checksum"})
       })
@Data
public class FileAsset {

    @Id
    @Column(name="id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name="owner_type", nullable=false, length=32)
    private OwnerType ownerType;

    @Column(name="owner_id", nullable=false)
    private Long ownerId;

    @Column(name="original_filename", nullable=false, length=255)
    private String originalFilename;

    @Column(name="content_type", length=255)
    private String contentType;

    @Column(name="size_bytes")
    private long sizeBytes;

    @Column(name="checksum", length=64)
    private String checksum;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name="data")
    private byte[] data;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    public FileAsset() {}

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
