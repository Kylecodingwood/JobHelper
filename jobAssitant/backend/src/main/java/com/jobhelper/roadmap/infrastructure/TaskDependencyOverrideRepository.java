package com.jobhelper.roadmap.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDependencyOverrideRepository extends JpaRepository<TaskDependencyOverrideEntity, UUID> {
    List<TaskDependencyOverrideEntity> findByTaskIdIn(List<UUID> taskIds);
}
