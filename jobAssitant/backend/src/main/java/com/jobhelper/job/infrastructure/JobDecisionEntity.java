package com.jobhelper.job.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_decision")
public class JobDecisionEntity {
    @Id
    @Column(name = "decision_id")
    private UUID decisionId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "decision_type", nullable = false)
    private String decisionType;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status")
    private String toStatus;

    @Column
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getDecisionId() { return decisionId; }
    public void setDecisionId(UUID decisionId) { this.decisionId = decisionId; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public String getDecisionType() { return decisionType; }
    public void setDecisionType(String decisionType) { this.decisionType = decisionType; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
