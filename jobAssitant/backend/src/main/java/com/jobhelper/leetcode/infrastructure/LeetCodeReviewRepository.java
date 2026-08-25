package com.jobhelper.leetcode.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeetCodeReviewRepository extends JpaRepository<LeetCodeReviewEntity, UUID> {
    Optional<LeetCodeReviewEntity> findByProblemId(UUID problemId);

    void deleteByProblemId(UUID problemId);
}
