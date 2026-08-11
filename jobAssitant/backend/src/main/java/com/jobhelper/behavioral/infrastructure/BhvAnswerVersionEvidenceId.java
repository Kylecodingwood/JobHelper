package com.jobhelper.behavioral.infrastructure;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class BhvAnswerVersionEvidenceId implements Serializable {
    private UUID versionId;
    private UUID evidenceRevisionId;

    public BhvAnswerVersionEvidenceId() {}

    public BhvAnswerVersionEvidenceId(UUID versionId, UUID evidenceRevisionId) {
        this.versionId = versionId;
        this.evidenceRevisionId = evidenceRevisionId;
    }

    public UUID getVersionId() { return versionId; }
    public void setVersionId(UUID versionId) { this.versionId = versionId; }
    public UUID getEvidenceRevisionId() { return evidenceRevisionId; }
    public void setEvidenceRevisionId(UUID evidenceRevisionId) { this.evidenceRevisionId = evidenceRevisionId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BhvAnswerVersionEvidenceId that)) return false;
        return Objects.equals(versionId, that.versionId)
                && Objects.equals(evidenceRevisionId, that.evidenceRevisionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionId, evidenceRevisionId);
    }
}
