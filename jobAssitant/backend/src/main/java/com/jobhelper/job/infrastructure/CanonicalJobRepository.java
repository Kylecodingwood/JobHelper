package com.jobhelper.job.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CanonicalJobRepository
        extends JpaRepository<CanonicalJobEntity, UUID>, JpaSpecificationExecutor<CanonicalJobEntity> {
    List<CanonicalJobEntity> findByJobStatusAndHiddenByDefaultFalseOrderByLastSeenAtDesc(String jobStatus, Pageable pageable);

    Optional<CanonicalJobEntity> findByPreferredSourceCodeAndSourceStableId(String preferredSourceCode, String sourceStableId);

    List<CanonicalJobEntity> findByCanonicalApplyUrlIsNotNull();

    List<CanonicalJobEntity> findByCompanyIgnoreCase(String company);

    long countByJobStatusAndHiddenByDefaultFalse(String jobStatus);
}
