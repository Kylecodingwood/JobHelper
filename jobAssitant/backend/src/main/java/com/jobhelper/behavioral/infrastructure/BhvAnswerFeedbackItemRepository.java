package com.jobhelper.behavioral.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BhvAnswerFeedbackItemRepository extends JpaRepository<BhvAnswerFeedbackItemEntity, UUID> {
    List<BhvAnswerFeedbackItemEntity> findByFeedbackIdOrderByItemIdAsc(UUID feedbackId);
}
