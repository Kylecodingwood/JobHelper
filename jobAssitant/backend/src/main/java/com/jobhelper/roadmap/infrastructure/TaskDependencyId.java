package com.jobhelper.roadmap.infrastructure;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class TaskDependencyId implements Serializable {
    private UUID predecessorTaskId;
    private UUID successorTaskId;

    public TaskDependencyId() {}

    public TaskDependencyId(UUID predecessorTaskId, UUID successorTaskId) {
        this.predecessorTaskId = predecessorTaskId;
        this.successorTaskId = successorTaskId;
    }

    public UUID getPredecessorTaskId() { return predecessorTaskId; }
    public void setPredecessorTaskId(UUID predecessorTaskId) { this.predecessorTaskId = predecessorTaskId; }
    public UUID getSuccessorTaskId() { return successorTaskId; }
    public void setSuccessorTaskId(UUID successorTaskId) { this.successorTaskId = successorTaskId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskDependencyId that)) return false;
        return Objects.equals(predecessorTaskId, that.predecessorTaskId)
                && Objects.equals(successorTaskId, that.successorTaskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predecessorTaskId, successorTaskId);
    }
}
