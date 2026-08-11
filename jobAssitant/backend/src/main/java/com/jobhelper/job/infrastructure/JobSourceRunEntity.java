package com.jobhelper.job.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_source_run")
public class JobSourceRunEntity {
    @Id
    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "trigger_type", nullable = false)
    private String triggerType;

    @Column(nullable = false)
    private String status;

    @Column(name = "parameters_snapshot")
    private String parametersSnapshot;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "received_count", nullable = false)
    private int receivedCount;

    @Column(name = "valid_count", nullable = false)
    private int validCount;

    @Column(name = "created_count", nullable = false)
    private int createdCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column
    private String diagnostics;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }
    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getParametersSnapshot() { return parametersSnapshot; }
    public void setParametersSnapshot(String parametersSnapshot) { this.parametersSnapshot = parametersSnapshot; }
    public int getRequestCount() { return requestCount; }
    public void setRequestCount(int requestCount) { this.requestCount = requestCount; }
    public int getReceivedCount() { return receivedCount; }
    public void setReceivedCount(int receivedCount) { this.receivedCount = receivedCount; }
    public int getValidCount() { return validCount; }
    public void setValidCount(int validCount) { this.validCount = validCount; }
    public int getCreatedCount() { return createdCount; }
    public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }
    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public String getDiagnostics() { return diagnostics; }
    public void setDiagnostics(String diagnostics) { this.diagnostics = diagnostics; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
