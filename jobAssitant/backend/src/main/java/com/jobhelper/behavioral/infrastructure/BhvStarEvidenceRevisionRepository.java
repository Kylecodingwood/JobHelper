package com.jobhelper.behavioral.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BhvStarEvidenceRevisionRepository extends JpaRepository<BhvStarEvidenceRevisionEntity, UUID> {
    List<BhvStarEvidenceRevisionEntity> findByEvidenceIdOrderByCreatedAtDesc(UUID evidenceId);
}
