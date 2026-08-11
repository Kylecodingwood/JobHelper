package com.jobhelper.job.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSourceRepository extends JpaRepository<JobSourceEntity, UUID> {
    Optional<JobSourceEntity> findByCode(String code);
}
