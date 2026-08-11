package com.jobhelper.roadmap.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDependencyRepository extends JpaRepository<TaskDependencyEntity, TaskDependencyId> {
    List<TaskDependencyEntity> findByPredecessorTaskIdIn(List<UUID> predecessorTaskIds);

    List<TaskDependencyEntity> findBySuccessorTaskIdIn(List<UUID> successorTaskIds);

    List<TaskDependencyEntity> findBySuccessorTaskId(UUID successorTaskId);

    boolean existsByPredecessorTaskIdAndSuccessorTaskId(UUID predecessorTaskId, UUID successorTaskId);

    void deleteByPredecessorTaskIdIn(List<UUID> predecessorTaskIds);
}
