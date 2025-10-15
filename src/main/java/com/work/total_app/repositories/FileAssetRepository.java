package com.work.total_app.repositories;

import com.work.total_app.models.file.FileAsset;
import com.work.total_app.models.file.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {

    boolean existsByOwnerTypeAndOwnerIdAndOriginalFilename(OwnerType ownerType, String ownerId, String originalFilename);

    List<FileAsset> findAllByOwnerTypeAndOwnerId(OwnerType ownerType, String ownerId);

    boolean existsByOwnerTypeAndOwnerIdAndChecksum(OwnerType type, String ownerId, String checksum);

    Optional<FileAsset> findByOwnerTypeAndOwnerIdAndChecksum(OwnerType type, String ownerId, String checksum);
}
