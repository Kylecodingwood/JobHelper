package com.jobhelper.roadmap.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapFolderRepository extends JpaRepository<RoadmapFolderEntity, UUID> {
    List<RoadmapFolderEntity> findAllByOrderBySortOrderAscUpdatedAtAsc();
    long count();
}
