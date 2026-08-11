package com.jobhelper.profile.infrastructure;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "target_role")
public class TargetRoleEntity {
    @Id
    @Column(name = "target_role_id")
    private UUID targetRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    private String seniority;
    private String location;

    @Column(name = "priority_order", nullable = false)
    private int priorityOrder;

    @Column(nullable = false)
    private boolean active = true;

    public UUID getTargetRoleId() { return targetRoleId; }
    public void setTargetRoleId(UUID targetRoleId) { this.targetRoleId = targetRoleId; }
    public ProfileEntity getProfile() { return profile; }
    public void setProfile(ProfileEntity profile) { this.profile = profile; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getSeniority() { return seniority; }
    public void setSeniority(String seniority) { this.seniority = seniority; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getPriorityOrder() { return priorityOrder; }
    public void setPriorityOrder(int priorityOrder) { this.priorityOrder = priorityOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
