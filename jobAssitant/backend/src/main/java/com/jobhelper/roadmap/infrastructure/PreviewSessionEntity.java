package com.jobhelper.roadmap.infrastructure;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Durable replacement for the in-memory preview token map (generate/recompute/template flows). */
@Entity
@Table(name = "preview_session")
public class PreviewSessionEntity {
    @Id
    @Column(name = "preview_token")
    private String previewToken;

    @Column(nullable = false)
    private String kind;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getPreviewToken() { return previewToken; }
    public void setPreviewToken(String previewToken) { this.previewToken = previewToken; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
