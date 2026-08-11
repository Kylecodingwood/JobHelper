package com.jobhelper.action.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Append-only decision history (action-model §4.6 `action_decision`, table added in V3 migration). */
@Entity
@Table(name = "action_decision")
public class ActionDecisionEntity {
    @Id
    @Column(name = "decision_id")
    private UUID decisionId;

    @Column(name = "action_id", nullable = false)
    private UUID actionId;

    @Column(nullable = false)
    private String operation;

    @Column
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getDecisionId() { return decisionId; }
    public void setDecisionId(UUID decisionId) { this.decisionId = decisionId; }
    public UUID getActionId() { return actionId; }
    public void setActionId(UUID actionId) { this.actionId = actionId; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
