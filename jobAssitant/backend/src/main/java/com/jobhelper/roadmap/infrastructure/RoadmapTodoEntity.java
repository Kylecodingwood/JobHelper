package com.jobhelper.roadmap.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roadmap_todo")
public class RoadmapTodoEntity {
    @Id
    @Column(name = "todo_id")
    private UUID todoId;

    @Column(name = "folder_id", nullable = false)
    private UUID folderId;

    @Column(nullable = false)
    private String name;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getTodoId() { return todoId; }
    public void setTodoId(UUID todoId) { this.todoId = todoId; }
    public UUID getFolderId() { return folderId; }
    public void setFolderId(UUID folderId) { this.folderId = folderId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
