package com.jobhelper.behavioral.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BhvAnswerVersionRepository extends JpaRepository<BhvAnswerVersionEntity, UUID> {
    List<BhvAnswerVersionEntity> findByAnswerIdOrderByVersionNumberAsc(UUID answerId);

    Optional<BhvAnswerVersionEntity> findTopByAnswerIdOrderByVersionNumberDesc(UUID answerId);
}
