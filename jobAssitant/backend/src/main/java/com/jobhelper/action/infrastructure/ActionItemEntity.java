package com.jobhelper.action.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "action_item")
public class ActionItemEntity {
    @Id
    @Column(name = "action_id")
    private UUID actionId;

    @Column(name = "source_domain", nullable = false)
    private String sourceDomain;

    @Column(name = "action_kind", nullable = false)
    private String actionKind;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private boolean active = true;

    private Instant deadline;

    @Column(nullable = false)
    private boolean pinned;

    @Column(name = "priority_band", nullable = false)
    private String priorityBand;

    @Column(name = "priority_sort_key", nullable = false)
    private String prioritySortKey;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "route_hint")
    private String routeHint;

    @Column(name = "focus_key")
    private String focusKey;

    @Column(name = "primary_reason")
    private String primaryReason;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getActionId() { return actionId; }
    public void setActionId(UUID actionId) { this.actionId = actionId; }
    public String getSourceDomain() { return sourceDomain; }
    public void setSourceDomain(String sourceDomain) { this.sourceDomain = sourceDomain; }
    public String getActionKind() { return actionKind; }
    public void setActionKind(String actionKind) { this.actionKind = actionKind; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public String getPriorityBand() { return priorityBand; }
    public void setPriorityBand(String priorityBand) { this.priorityBand = priorityBand; }
    public String getPrioritySortKey() { return prioritySortKey; }
    public void setPrioritySortKey(String prioritySortKey) { this.prioritySortKey = prioritySortKey; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public String getRouteHint() { return routeHint; }
    public void setRouteHint(String routeHint) { this.routeHint = routeHint; }
    public String getFocusKey() { return focusKey; }
    public void setFocusKey(String focusKey) { this.focusKey = focusKey; }
    public String getPrimaryReason() { return primaryReason; }
    public void setPrimaryReason(String primaryReason) { this.primaryReason = primaryReason; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
