package com.jobhelper.behavioral.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bhv_star_evidence_revision")
public class BhvStarEvidenceRevisionEntity {
    @Id
    @Column(name = "revision_id")
    private UUID revisionId;

    @Column(name = "evidence_id", nullable = false)
    private UUID evidenceId;

    @Column(name = "parent_revision_id")
    private UUID parentRevisionId;

    @Column(name = "situation")
    private String situation;

    @Column(name = "task")
    private String task;

    @Column(name = "action")
    private String action;

    @Column(name = "result")
    private String result;

    @Column(name = "competency_tags")
    private String competencyTags;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getRevisionId() { return revisionId; }
    public void setRevisionId(UUID revisionId) { this.revisionId = revisionId; }
    public UUID getEvidenceId() { return evidenceId; }
    public void setEvidenceId(UUID evidenceId) { this.evidenceId = evidenceId; }
    public UUID getParentRevisionId() { return parentRevisionId; }
    public void setParentRevisionId(UUID parentRevisionId) { this.parentRevisionId = parentRevisionId; }
    public String getSituation() { return situation; }
    public void setSituation(String situation) { this.situation = situation; }
    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getCompetencyTags() { return competencyTags; }
    public void setCompetencyTags(String competencyTags) { this.competencyTags = competencyTags; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
