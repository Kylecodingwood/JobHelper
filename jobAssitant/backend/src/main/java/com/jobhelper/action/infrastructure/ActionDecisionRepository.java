package com.jobhelper.action.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionDecisionRepository extends JpaRepository<ActionDecisionEntity, UUID> {
    List<ActionDecisionEntity> findByActionIdOrderByCreatedAtDesc(UUID actionId);
}
