package com.jobhelper.roadmap.infrastructure;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_dependency")
@IdClass(TaskDependencyId.class)
public class TaskDependencyEntity {
    @Id
    @Column(name = "predecessor_task_id")
    private UUID predecessorTaskId;

    @Id
    @Column(name = "successor_task_id")
    private UUID successorTaskId;

    public TaskDependencyEntity() {}

    public TaskDependencyEntity(UUID predecessorTaskId, UUID successorTaskId) {
        this.predecessorTaskId = predecessorTaskId;
        this.successorTaskId = successorTaskId;
    }

    public UUID getPredecessorTaskId() { return predecessorTaskId; }
    public void setPredecessorTaskId(UUID predecessorTaskId) { this.predecessorTaskId = predecessorTaskId; }
    public UUID getSuccessorTaskId() { return successorTaskId; }
    public void setSuccessorTaskId(UUID successorTaskId) { this.successorTaskId = successorTaskId; }
}
