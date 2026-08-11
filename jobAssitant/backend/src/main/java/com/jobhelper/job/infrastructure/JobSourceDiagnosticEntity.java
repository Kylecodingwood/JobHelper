package com.jobhelper.job.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_source_diagnostic")
public class JobSourceDiagnosticEntity {
    @Id
    @Column(name = "diagnostic_id")
    private UUID diagnosticId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(nullable = false)
    private String category;

    @Column
    private String site;

    @Column
    private String message;

    @Column(name = "item_key")
    private String itemKey;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public UUID getDiagnosticId() { return diagnosticId; }
    public void setDiagnosticId(UUID diagnosticId) { this.diagnosticId = diagnosticId; }
    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
