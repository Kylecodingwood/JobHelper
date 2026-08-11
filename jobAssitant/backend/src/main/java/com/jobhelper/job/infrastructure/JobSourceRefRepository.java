package com.jobhelper.job.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSourceRefRepository extends JpaRepository<JobSourceRefEntity, UUID> {
    List<JobSourceRefEntity> findByJobIdOrderByLastSeenAtDesc(UUID jobId);

    Optional<JobSourceRefEntity> findBySourceCodeAndSourceStableId(String sourceCode, String sourceStableId);

    long countByJobId(UUID jobId);
}
