package com.jobhelper.behavioral.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BhvAnswerVersionEvidenceRepository extends JpaRepository<BhvAnswerVersionEvidenceEntity, BhvAnswerVersionEvidenceId> {
    List<BhvAnswerVersionEvidenceEntity> findByVersionId(UUID versionId);
}
