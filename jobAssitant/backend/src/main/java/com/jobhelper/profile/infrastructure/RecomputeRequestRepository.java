package com.jobhelper.profile.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecomputeRequestRepository extends JpaRepository<RecomputeRequestEntity, UUID> {
    Optional<RecomputeRequestEntity> findFirstByStatusInOrderByCreatedAtDesc(List<String> statuses);

    List<RecomputeRequestEntity> findByStatusIn(List<String> statuses);
}
