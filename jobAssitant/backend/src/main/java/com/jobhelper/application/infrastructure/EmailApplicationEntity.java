package com.jobhelper.application.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_application")
public class EmailApplicationEntity {
    @Id @Column(name = "app_id") private UUID appId;
    @Column(name = "company_name", nullable = false) private String companyName;
    @Column(name = "role_name", nullable = false, columnDefinition = "TEXT") private String roleName;
    @Column(nullable = false) private String category;
    @Column(name = "is_swe", nullable = false) private boolean isSwe;
    @Column(nullable = false) private String status;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public UUID getAppId() { return appId; }
    public void setAppId(UUID appId) { this.appId = appId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isIsSwe() { return isSwe; }
    public void setIsSwe(boolean isSwe) { this.isSwe = isSwe; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
