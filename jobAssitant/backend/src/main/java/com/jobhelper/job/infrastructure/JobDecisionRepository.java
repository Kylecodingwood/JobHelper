package com.jobhelper.job.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDecisionRepository extends JpaRepository<JobDecisionEntity, UUID> {
    List<JobDecisionEntity> findByJobIdOrderByCreatedAtDesc(UUID jobId);
}
