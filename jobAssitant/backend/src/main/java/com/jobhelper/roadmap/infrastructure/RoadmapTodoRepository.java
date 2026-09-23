package com.jobhelper.roadmap.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapTodoRepository extends JpaRepository<RoadmapTodoEntity, UUID> {
    List<RoadmapTodoEntity> findByFolderId(UUID folderId);

    long countByFolderId(UUID folderId);

    List<RoadmapTodoEntity> findByDoneFalseOrderBySortOrderAscUpdatedAtDesc();
}
