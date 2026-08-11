package com.jobhelper.job.infrastructure;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_possible_duplicate")
public class JobPossibleDuplicateEntity {
    @Id
    @Column(name = "duplicate_id")
    private UUID duplicateId;

    @Column(name = "left_job_id", nullable = false)
    private UUID leftJobId;

    @Column(name = "right_job_id", nullable = false)
    private UUID rightJobId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "signals", columnDefinition = "jsonb", nullable = false)
    private String signals;

    @Column(nullable = false)
    private String status;

    @Column(name = "decided_by_decision_id")
    private UUID decidedByDecisionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getDuplicateId() { return duplicateId; }
    public void setDuplicateId(UUID duplicateId) { this.duplicateId = duplicateId; }
    public UUID getLeftJobId() { return leftJobId; }
    public void setLeftJobId(UUID leftJobId) { this.leftJobId = leftJobId; }
    public UUID getRightJobId() { return rightJobId; }
    public void setRightJobId(UUID rightJobId) { this.rightJobId = rightJobId; }
    public String getSignals() { return signals; }
    public void setSignals(String signals) { this.signals = signals; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getDecidedByDecisionId() { return decidedByDecisionId; }
    public void setDecidedByDecisionId(UUID decidedByDecisionId) { this.decidedByDecisionId = decidedByDecisionId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
