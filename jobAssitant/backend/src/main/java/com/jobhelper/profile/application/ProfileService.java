package com.jobhelper.profile.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.profile.api.ProfileDtos.EducationPeriodDto;
import com.jobhelper.profile.api.ProfileDtos.ImpactSummary;
import com.jobhelper.profile.api.ProfileDtos.LanguageProficiencyDto;
import com.jobhelper.profile.api.ProfileDtos.ProfileAggregateDto;
import com.jobhelper.profile.api.ProfileDtos.ProfileWriteRequest;
import com.jobhelper.profile.api.ProfileDtos.ProfileWriteResponse;
import com.jobhelper.profile.api.ProfileDtos.WorkExperienceDto;
import com.jobhelper.profile.infrastructure.EducationPeriodEntity;
import com.jobhelper.profile.infrastructure.LanguageProficiencyEntity;
import com.jobhelper.profile.infrastructure.ProfileEntity;
import com.jobhelper.profile.infrastructure.ProfileRepository;
import com.jobhelper.profile.infrastructure.ProfileSkillEntity;
import com.jobhelper.profile.infrastructure.WorkExperienceEntity;
import com.jobhelper.shared.outbox.OutboxService;
import com.jobhelper.shared.pipeline.ActionProjector;
import com.jobhelper.shared.web.ApiException;

@Service
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final OutboxService outboxService;
    private final ActionProjector actionProjector;

    public ProfileService(
            ProfileRepository profileRepository,
            OutboxService outboxService,
            ActionProjector actionProjector) {
        this.profileRepository = profileRepository;
        this.outboxService = outboxService;
        this.actionProjector = actionProjector;
    }

    @Transactional(readOnly = true)
    public boolean exists() {
        return profileRepository.findSingleton().isPresent();
    }

    @Transactional(readOnly = true)
    public ProfileAggregateDto getProfile() {
        ProfileEntity profile = profileRepository.findSingleton()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile not found"));
        return toDto(profile);
    }

    @Transactional
    public ProfileWriteResponse save(ProfileWriteRequest request) {
        validateOrThrow(request);
        ProfileEntity profile = profileRepository.findSingleton().orElseGet(this::newProfile);
        if (profile.getProfileId() != null) {
            if (request.expectedProfileVersion() == null
                    || !request.expectedProfileVersion().equals(profile.getProfileVersion())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "PROFILE_VERSION_CONFLICT",
                        "Profile version mismatch",
                        Map.of("currentProfileVersion", profile.getProfileVersion()));
            }
        }
        apply(profile, request);
        profile.setProfileVersion(profile.getProfileVersion() == null ? 1 : profile.getProfileVersion() + 1);
        profile.setLifecycle("READY");
        profile.setUpdatedAt(Instant.now());
        // clear legacy collections
        profile.getTargetRoles().clear();
        profile.getWorkAuthorizations().clear();
        profileRepository.save(profile);
        outboxService.append("PROFILE", "PROFILE_UPDATED", "PROFILE", profile.getProfileId(),
                "{\"profileVersion\":" + profile.getProfileVersion() + "}");
        actionProjector.onProfileReady();
        return new ProfileWriteResponse(
                profile.getProfileVersion(),
                profile.getLifecycle(),
                new ImpactSummary(List.of("PROFILE"), "Persona saved; does not update Roadmap or Source searchTerms"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validate(ProfileWriteRequest request) {
        List<String> errors = new ArrayList<>();
        collectErrors(request, errors);
        return Map.of("valid", errors.isEmpty(), "errors", errors);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> fieldUsage() {
        Map<String, Object> map = new HashMap<>();
        map.put("nationality", "Persona / future AI prompt");
        map.put("identityStatus", "Gate WORK_AUTH + persona");
        map.put("identityValidUntil", "Gate WORK_AUTH");
        map.put("targetCountry", "Persona / location preference");
        map.put("jobSeekingGoal", "Persona / future AI prompt");
        map.put("educationPeriods", "Persona");
        map.put("workExperiences", "Persona / future AI prompt");
        map.put("skills", "Rank SKILL + persona");
        map.put("languageProficiencies", "Gate LANGUAGE");
        map.put("searchTerms", "Owned globally under Job Sources — shared by all sync adapters");
        return map;
    }

    private void validateOrThrow(ProfileWriteRequest request) {
        List<String> errors = new ArrayList<>();
        collectErrors(request, errors);
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Profile invalid", Map.of("errors", errors));
        }
    }

    private void collectErrors(ProfileWriteRequest request, List<String> errors) {
        if (request.identityStatus() == null || request.identityStatus().isBlank()) {
            errors.add("identityStatus required");
        }
        if (request.languageProficiencies() == null || request.languageProficiencies().isEmpty()) {
            errors.add("at least one language required");
        }
    }

    private void apply(ProfileEntity profile, ProfileWriteRequest request) {
        profile.setNationality(request.nationality());
        profile.setIdentityStatus(request.identityStatus());
        profile.setIdentityValidUntil(request.identityValidUntil());
        profile.setTargetCountry(request.targetCountry());
        profile.setJobSeekingGoal(request.jobSeekingGoal());

        profile.getEducationPeriods().clear();
        for (EducationPeriodDto dto : nullSafe(request.educationPeriods())) {
            EducationPeriodEntity e = new EducationPeriodEntity();
            e.setEducationPeriodId(dto.educationPeriodId() != null ? dto.educationPeriodId() : UUID.randomUUID());
            e.setProfile(profile);
            e.setInstitutionName(dto.institutionName());
            e.setProgrammeName(dto.programmeName());
            e.setStartDate(dto.startDate());
            e.setExpectedGraduationDate(dto.expectedGraduationDate());
            e.setCountryCode(dto.countryCode());
            e.setPrimary(Boolean.TRUE.equals(dto.isPrimary()));
            profile.getEducationPeriods().add(e);
        }

        profile.getWorkExperiences().clear();
        int wi = 0;
        for (WorkExperienceDto dto : nullSafe(request.workExperiences())) {
            if (dto.company() == null || dto.company().isBlank()) {
                continue;
            }
            WorkExperienceEntity e = new WorkExperienceEntity();
            e.setWorkExperienceId(dto.workExperienceId() != null ? dto.workExperienceId() : UUID.randomUUID());
            e.setProfile(profile);
            e.setCompany(dto.company());
            e.setTitle(dto.title());
            e.setStartDate(dto.startDate());
            e.setEndDate(dto.endDate());
            e.setSummary(dto.summary());
            e.setSortOrder(wi++);
            profile.getWorkExperiences().add(e);
        }

        profile.getSkills().clear();
        int si = 0;
        for (String skill : nullSafe(request.skills())) {
            if (skill == null || skill.isBlank()) {
                continue;
            }
            ProfileSkillEntity e = new ProfileSkillEntity();
            e.setProfileSkillId(UUID.randomUUID());
            e.setProfile(profile);
            e.setSkillName(skill.trim());
            e.setSortOrder(si++);
            profile.getSkills().add(e);
        }

        profile.getLanguageProficiencies().clear();
        for (LanguageProficiencyDto dto : nullSafe(request.languageProficiencies())) {
            LanguageProficiencyEntity e = new LanguageProficiencyEntity();
            e.setLanguageProficiencyId(dto.languageProficiencyId() != null ? dto.languageProficiencyId() : UUID.randomUUID());
            e.setProfile(profile);
            e.setLanguageCode(dto.languageCode());
            e.setProficiency(dto.proficiency());
            e.setEvidenceRef(dto.evidenceRef());
            e.setNotes(dto.notes());
            profile.getLanguageProficiencies().add(e);
        }
    }

    private ProfileEntity newProfile() {
        ProfileEntity p = new ProfileEntity();
        p.setProfileId(UUID.randomUUID());
        p.setProfileVersion(0);
        p.setLifecycle("DRAFT");
        Instant now = Instant.now();
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return p;
    }

    private ProfileAggregateDto toDto(ProfileEntity p) {
        List<EducationPeriodDto> edu = p.getEducationPeriods().stream()
                .map(e -> new EducationPeriodDto(e.getEducationPeriodId(), e.getInstitutionName(), e.getProgrammeName(),
                        e.getStartDate(), e.getExpectedGraduationDate(), e.getCountryCode(), e.isPrimary()))
                .toList();
        List<WorkExperienceDto> exp = p.getWorkExperiences().stream()
                .map(e -> new WorkExperienceDto(e.getWorkExperienceId(), e.getCompany(), e.getTitle(),
                        e.getStartDate(), e.getEndDate(), e.getSummary()))
                .toList();
        List<String> skills = p.getSkills().stream().map(ProfileSkillEntity::getSkillName).toList();
        List<LanguageProficiencyDto> langs = p.getLanguageProficiencies().stream()
                .map(e -> new LanguageProficiencyDto(e.getLanguageProficiencyId(), e.getLanguageCode(), e.getProficiency(),
                        e.getEvidenceRef(), e.getNotes()))
                .toList();
        return new ProfileAggregateDto(
                p.getProfileId(),
                p.getProfileVersion(),
                p.getLifecycle(),
                p.getNationality(),
                p.getIdentityStatus(),
                p.getIdentityValidUntil(),
                p.getTargetCountry(),
                p.getJobSeekingGoal(),
                edu,
                exp,
                skills,
                langs);
    }

    private <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
