package com.jobhelper.roadmap.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationRecordRepository extends JpaRepository<GenerationRecordEntity, UUID> {
    Optional<GenerationRecordEntity> findFirstByInputHashAndModeOrderByCreatedAtDesc(String inputHash, String mode);

    Optional<GenerationRecordEntity> findFirstByPreviewTokenOrderByCreatedAtDesc(String previewToken);
}
