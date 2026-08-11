package com.jobhelper.roadmap.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateTaskDefinitionRepository extends JpaRepository<TemplateTaskDefinitionEntity, UUID> {
    List<TemplateTaskDefinitionEntity> findByTemplateVersionIdOrderBySortOrderAsc(UUID templateVersionId);
}
