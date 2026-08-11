package com.jobhelper.roadmap.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "generation_record")
public class GenerationRecordEntity {
    @Id
    @Column(name = "generation_record_id")
    private UUID generationRecordId;

    @Column(name = "roadmap_id", nullable = false)
    private UUID roadmapId;

    @Column(nullable = false)
    private String mode;

    @Column(name = "input_hash", nullable = false)
    private String inputHash;

    @Column(name = "profile_version", nullable = false)
    private int profileVersion;

    @Column(name = "preview_token")
    private String previewToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getGenerationRecordId() { return generationRecordId; }
    public void setGenerationRecordId(UUID generationRecordId) { this.generationRecordId = generationRecordId; }
    public UUID getRoadmapId() { return roadmapId; }
    public void setRoadmapId(UUID roadmapId) { this.roadmapId = roadmapId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getInputHash() { return inputHash; }
    public void setInputHash(String inputHash) { this.inputHash = inputHash; }
    public int getProfileVersion() { return profileVersion; }
    public void setProfileVersion(int profileVersion) { this.profileVersion = profileVersion; }
    public String getPreviewToken() { return previewToken; }
    public void setPreviewToken(String previewToken) { this.previewToken = previewToken; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
