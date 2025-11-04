package com.work.total_app.repositories;

import com.work.total_app.models.backup.BackupMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackupMetadataRepository extends JpaRepository<BackupMetadata, Long> {
    Optional<BackupMetadata> findByBackupName(String backupName);
}

