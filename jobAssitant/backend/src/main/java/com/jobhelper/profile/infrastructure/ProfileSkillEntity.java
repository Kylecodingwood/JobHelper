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
@Table(name = "profile_skill")
public class ProfileSkillEntity {
    @Id
    @Column(name = "profile_skill_id")
    private UUID profileSkillId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public UUID getProfileSkillId() { return profileSkillId; }
    public void setProfileSkillId(UUID profileSkillId) { this.profileSkillId = profileSkillId; }
    public ProfileEntity getProfile() { return profile; }
    public void setProfile(ProfileEntity profile) { this.profile = profile; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
