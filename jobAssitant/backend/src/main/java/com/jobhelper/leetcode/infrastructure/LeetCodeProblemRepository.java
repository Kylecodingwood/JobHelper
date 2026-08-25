package com.jobhelper.leetcode.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeetCodeProblemRepository extends JpaRepository<LeetCodeProblemEntity, UUID> {
    List<LeetCodeProblemEntity> findAllByOrderBySortOrderAscProblemNumberAsc();

    Optional<LeetCodeProblemEntity> findByProblemNumber(int problemNumber);

    long count();
}
