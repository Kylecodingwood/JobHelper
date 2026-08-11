package com.jobhelper.action.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionItemRepository extends JpaRepository<ActionItemEntity, UUID> {
    List<ActionItemEntity> findByActiveTrueAndStatusOrderByPrioritySortKeyAsc(String status, Pageable pageable);

    List<ActionItemEntity> findByStatusOrderByPrioritySortKeyAsc(String status, Pageable pageable);

    List<ActionItemEntity> findByActiveTrueAndActionKind(String actionKind);

    List<ActionItemEntity> findByActiveTrueAndActionKindIn(List<String> actionKinds);
}
