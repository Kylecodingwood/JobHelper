package com.jobhelper.job.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSourceDiagnosticRepository extends JpaRepository<JobSourceDiagnosticEntity, UUID> {
    List<JobSourceDiagnosticEntity> findByRunIdOrderByOccurredAtAsc(UUID runId);
}
