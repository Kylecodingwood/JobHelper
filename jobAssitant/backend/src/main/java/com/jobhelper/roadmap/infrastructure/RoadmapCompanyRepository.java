package com.jobhelper.roadmap.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapCompanyRepository extends JpaRepository<RoadmapCompanyEntity, UUID> {
    List<RoadmapCompanyEntity> findByFolderIdOrderBySortOrderAscUpdatedAtDesc(UUID folderId);

    long countByFolderId(UUID folderId);

    List<RoadmapCompanyEntity> findByStatusOrderByUpdatedAtDesc(String status);
}
