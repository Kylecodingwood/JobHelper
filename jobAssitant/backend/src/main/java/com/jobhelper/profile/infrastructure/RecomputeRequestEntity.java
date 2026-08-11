package com.jobhelper.profile.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recompute_request")
public class RecomputeRequestEntity {
    @Id
    @Column(name = "recompute_request_id")
    private UUID recomputeRequestId;

    @Column(name = "profile_version", nullable = false)
    private int profileVersion;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, length = 1024)
    private String scopes;

    @Column(name = "change_summary", length = 4000)
    private String changeSummary;

    @Column(name = "impact_summary", length = 4000)
    private String impactSummary;

    @Column(name = "preview_token")
    private String previewToken;

    @Column(name = "preview_expires_at")
    private Instant previewExpiresAt;

    @Column(name = "preview_payload", length = 16000)
    private String previewPayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public UUID getRecomputeRequestId() { return recomputeRequestId; }
    public void setRecomputeRequestId(UUID recomputeRequestId) { this.recomputeRequestId = recomputeRequestId; }
    public int getProfileVersion() { return profileVersion; }
    public void setProfileVersion(int profileVersion) { this.profileVersion = profileVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
    public String getImpactSummary() { return impactSummary; }
    public void setImpactSummary(String impactSummary) { this.impactSummary = impactSummary; }
    public String getPreviewToken() { return previewToken; }
    public void setPreviewToken(String previewToken) { this.previewToken = previewToken; }
    public Instant getPreviewExpiresAt() { return previewExpiresAt; }
    public void setPreviewExpiresAt(Instant previewExpiresAt) { this.previewExpiresAt = previewExpiresAt; }
    public String getPreviewPayload() { return previewPayload; }
    public void setPreviewPayload(String previewPayload) { this.previewPayload = previewPayload; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
}
