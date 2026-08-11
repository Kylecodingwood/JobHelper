package com.jobhelper.job.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_source_ref")
public class JobSourceRefEntity {
    @Id
    @Column(name = "job_source_ref_id")
    private UUID jobSourceRefId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "source_code", nullable = false)
    private String sourceCode;

    @Column(name = "source_stable_id")
    private String sourceStableId;

    @Column(name = "apply_url", length = 1024)
    private String applyUrl;

    @Column(name = "is_preferred", nullable = false)
    private boolean preferred;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getJobSourceRefId() { return jobSourceRefId; }
    public void setJobSourceRefId(UUID jobSourceRefId) { this.jobSourceRefId = jobSourceRefId; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public String getSourceStableId() { return sourceStableId; }
    public void setSourceStableId(String sourceStableId) { this.sourceStableId = sourceStableId; }
    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }
    public boolean isPreferred() { return preferred; }
    public void setPreferred(boolean preferred) { this.preferred = preferred; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Instant firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
