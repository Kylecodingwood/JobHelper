package com.jobhelper.roadmap.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapTemplateVersionRepository extends JpaRepository<RoadmapTemplateVersionEntity, UUID> {
    List<RoadmapTemplateVersionEntity> findByTemplateIdOrderByVersionDesc(UUID templateId);

    Optional<RoadmapTemplateVersionEntity> findFirstByTemplateIdAndStatusOrderByVersionDesc(UUID templateId, String status);

    Optional<RoadmapTemplateVersionEntity> findFirstByTemplateIdOrderByVersionDesc(UUID templateId);
}
