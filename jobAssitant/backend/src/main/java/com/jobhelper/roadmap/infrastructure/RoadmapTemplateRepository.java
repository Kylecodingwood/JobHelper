package com.jobhelper.roadmap.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapTemplateRepository extends JpaRepository<RoadmapTemplateEntity, UUID> {
    Optional<RoadmapTemplateEntity> findByCode(String code);

    List<RoadmapTemplateEntity> findByType(String type);
}
