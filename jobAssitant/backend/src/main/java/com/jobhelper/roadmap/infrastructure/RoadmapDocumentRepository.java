package com.jobhelper.roadmap.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapDocumentRepository extends JpaRepository<RoadmapDocumentEntity, UUID> {
    List<RoadmapDocumentEntity> findByFolderIdOrderBySortOrderAscUpdatedAtDesc(UUID folderId);

    long countByFolderId(UUID folderId);
}
