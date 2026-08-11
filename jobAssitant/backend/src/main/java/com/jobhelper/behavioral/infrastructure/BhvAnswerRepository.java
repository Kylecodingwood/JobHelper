package com.jobhelper.behavioral.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BhvAnswerRepository extends JpaRepository<BhvAnswerEntity, UUID> {
    List<BhvAnswerEntity> findByQuestionIdOrderByCreatedAtDesc(UUID questionId);
}
