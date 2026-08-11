package com.jobhelper.behavioral.infrastructure;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "bhv_answer_version_evidence")
@IdClass(BhvAnswerVersionEvidenceId.class)
public class BhvAnswerVersionEvidenceEntity {
    @Id
    @Column(name = "version_id")
    private UUID versionId;

    @Id
    @Column(name = "evidence_revision_id")
    private UUID evidenceRevisionId;

    public BhvAnswerVersionEvidenceEntity() {}

    public BhvAnswerVersionEvidenceEntity(UUID versionId, UUID evidenceRevisionId) {
        this.versionId = versionId;
        this.evidenceRevisionId = evidenceRevisionId;
    }

    public UUID getVersionId() { return versionId; }
    public void setVersionId(UUID versionId) { this.versionId = versionId; }
    public UUID getEvidenceRevisionId() { return evidenceRevisionId; }
    public void setEvidenceRevisionId(UUID evidenceRevisionId) { this.evidenceRevisionId = evidenceRevisionId; }
}
