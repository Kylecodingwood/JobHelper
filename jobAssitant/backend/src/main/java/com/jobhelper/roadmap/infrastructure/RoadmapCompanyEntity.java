package com.jobhelper.roadmap.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roadmap_company")
public class RoadmapCompanyEntity {
    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "folder_id", nullable = false)
    private UUID folderId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String status;

    @Column
    private String contact;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getFolderId() { return folderId; }
    public void setFolderId(UUID folderId) { this.folderId = folderId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
