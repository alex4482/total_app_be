package com.work.total_app.repositories;

import com.work.total_app.models.file.FileAsset;
import com.work.total_app.models.file.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for persisting and querying FileAsset entities.
 * Includes convenience exists/find methods used for name uniqueness and checksum-based deduplication.
 */
@Repository
public interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {

    /** Check whether an owner already has a file with the given original filename. */
    boolean existsByOwnerTypeAndOwnerIdAndOriginalFilename(OwnerType ownerType, Long ownerId, String originalFilename);

    /** List all files belonging to an owner. */
    List<FileAsset> findAllByOwnerTypeAndOwnerId(OwnerType ownerType, Long ownerId);

    /** Check whether an owner already has a file with the given checksum. */
    boolean existsByOwnerTypeAndOwnerIdAndChecksum(OwnerType type, Long ownerId, String checksum);

    /** Find an owner's file by checksum (used for deduplication flow). */
    Optional<FileAsset> findByOwnerTypeAndOwnerIdAndChecksum(OwnerType type, Long ownerId, String checksum);

    /** Delete all files belonging to an owner. */
    void deleteAllByOwnerTypeAndOwnerId(OwnerType ownerType, Long ownerId);
}
