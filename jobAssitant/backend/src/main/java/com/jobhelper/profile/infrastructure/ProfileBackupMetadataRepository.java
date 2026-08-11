package com.jobhelper.profile.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileBackupMetadataRepository extends JpaRepository<ProfileBackupMetadataEntity, UUID> {
    List<ProfileBackupMetadataEntity> findByBackupTypeOrderByCreatedAtDesc(String backupType);

    List<ProfileBackupMetadataEntity> findAllByOrderByCreatedAtDesc();
}
