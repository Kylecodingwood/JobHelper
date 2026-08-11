package com.jobhelper.roadmap.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "roadmap_task")
public class RoadmapTaskEntity {
    @Id
    @Column(name = "task_id")
    private UUID taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private RoadmapEntity roadmap;

    @Column(name = "logical_task_key", nullable = false)
    private String logicalTaskKey;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String phase;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String actionability;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String priority;

    @Column(name = "user_pinned", nullable = false)
    private boolean userPinned;

    @Column(name = "user_edited", nullable = false)
    private boolean userEdited;

    @Column(name = "completion_criteria")
    private String completionCriteria;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }
    public RoadmapEntity getRoadmap() { return roadmap; }
    public void setRoadmap(RoadmapEntity roadmap) { this.roadmap = roadmap; }
    public String getLogicalTaskKey() { return logicalTaskKey; }
    public void setLogicalTaskKey(String logicalTaskKey) { this.logicalTaskKey = logicalTaskKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getActionability() { return actionability; }
    public void setActionability(String actionability) { this.actionability = actionability; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public boolean isUserPinned() { return userPinned; }
    public void setUserPinned(boolean userPinned) { this.userPinned = userPinned; }
    public boolean isUserEdited() { return userEdited; }
    public void setUserEdited(boolean userEdited) { this.userEdited = userEdited; }
    public String getCompletionCriteria() { return completionCriteria; }
    public void setCompletionCriteria(String completionCriteria) { this.completionCriteria = completionCriteria; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
