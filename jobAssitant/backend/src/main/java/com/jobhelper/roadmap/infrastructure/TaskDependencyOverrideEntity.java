package com.jobhelper.roadmap.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_dependency_override")
public class TaskDependencyOverrideEntity {
    @Id
    @Column(name = "task_id")
    private UUID taskId;

    @Column(nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
