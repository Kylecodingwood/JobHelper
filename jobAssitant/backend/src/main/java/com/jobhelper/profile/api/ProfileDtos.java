package com.jobhelper.profile.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProfileDtos {
    private ProfileDtos() {}

    public record EducationPeriodDto(
            UUID educationPeriodId,
            String institutionName,
            String programmeName,
            LocalDate startDate,
            LocalDate expectedGraduationDate,
            String countryCode,
            Boolean isPrimary) {}

    public record WorkExperienceDto(
            UUID workExperienceId,
            String company,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            String summary) {}

    public record LanguageProficiencyDto(
            UUID languageProficiencyId,
            String languageCode,
            String proficiency,
            String evidenceRef,
            String notes) {}

    public record ProfileAggregateDto(
            UUID profileId,
            Integer profileVersion,
            String lifecycle,
            String nationality,
            String identityStatus,
            LocalDate identityValidUntil,
            String targetCountry,
            String jobSeekingGoal,
            List<EducationPeriodDto> educationPeriods,
            List<WorkExperienceDto> workExperiences,
            List<String> skills,
            List<LanguageProficiencyDto> languageProficiencies) {}

    public record ProfileWriteRequest(
            Integer expectedProfileVersion,
            String nationality,
            String identityStatus,
            LocalDate identityValidUntil,
            String targetCountry,
            String jobSeekingGoal,
            List<EducationPeriodDto> educationPeriods,
            List<WorkExperienceDto> workExperiences,
            List<String> skills,
            List<LanguageProficiencyDto> languageProficiencies) {}

    public record ProfileWriteResponse(
            Integer profileVersion,
            String lifecycle,
            ImpactSummary impactSummary) {}

    public record ImpactSummary(List<String> affectedScopes, String message) {}
}
