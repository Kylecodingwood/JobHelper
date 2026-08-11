package com.jobhelper.job.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSourceRunRepository extends JpaRepository<JobSourceRunEntity, UUID> {
    List<JobSourceRunEntity> findTop20ByOrderByCreatedAtDesc();

    boolean existsBySourceIdAndStatus(UUID sourceId, String status);

    Optional<JobSourceRunEntity> findTopBySourceIdOrderByCreatedAtDesc(UUID sourceId);
}
