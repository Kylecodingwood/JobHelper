package com.jobhelper.job.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.profile.infrastructure.ProfileEntity;

class GateRankServiceTest {

    private final GateRankService service = new GateRankService();

    @Test
    void workAuthMissingExpectedStartDate_needsConfirmation() {
        CanonicalJobEntity job = baseJob("Graduate Software Engineer", "Dublin, Ireland");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        assertEquals("NEEDS_CONFIRMATION", job.getGateStatus());
        assertTrue(job.getGateDimensionsJson().contains("WORK_AUTH"));
        assertTrue(job.getGateDimensionsJson().contains("expectedStartDate missing"));
        assertFalse(job.isHiddenByDefault());
    }

    @Test
    void roleGate_failsNonSoftwareGraduate() {
        CanonicalJobEntity job = baseJob("Accounting Graduate", "Dublin, Ireland");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        assertEquals("FAILED", job.getGateStatus());
        assertTrue(job.isHiddenByDefault());
        assertTrue(job.getGateDimensionsJson().contains("ROLE"));
        assertTrue(job.getGateDimensionsJson().contains("FAIL"));
    }

    @Test
    void roleGate_passesSoftwareEngineer() {
        CanonicalJobEntity job = baseJob("Software Engineer – AI-Assisted Full-Stack Development", "Dublin");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        assertEquals("NEEDS_CONFIRMATION", job.getGateStatus()); // WORK_AUTH still open
        assertFalse(job.isHiddenByDefault());
        assertTrue(job.getGateDimensionsJson().contains("\"dimension\":\"ROLE\",\"status\":\"PASS\"")
                || job.getGateDimensionsJson().contains("ROLE"));
        assertTrue(job.getGateDimensionsJson().contains("software-role signal"));
    }

    @Test
    void roleGate_failsTechnicalWriterEvenIfJdMentionsSoftware() {
        CanonicalJobEntity job = baseJob("Technical Writer", "Dublin, Ireland");
        job.setDescription("Write docs for our software platform and APIs.");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        assertEquals("FAILED", job.getGateStatus());
        assertTrue(job.isHiddenByDefault());
    }

    @Test
    void roleGate_failsCustomerSupportEngineer() {
        CanonicalJobEntity job = baseJob("Customer Support Engineer", "Dublin, Ireland");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        assertEquals("FAILED", job.getGateStatus());
    }

    @Test
    void seniorityGate_yearsInJdAreSoftNotHardHide() {
        CanonicalJobEntity job = baseJob("Software Engineer", "Dublin, Ireland");
        job.setDescription("We need someone with 5 years of experience building APIs.");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        // Soft Cap: years-in-JD alone must not hard-hide a generic SWE title.
        assertEquals("NEEDS_CONFIRMATION", job.getGateStatus());
        assertFalse(job.isHiddenByDefault());
        assertTrue(job.getGateDimensionsJson().contains("SENIORITY"));
        assertTrue(job.getGateDimensionsJson().contains("MID_OK")
                || job.getGateDimensionsJson().contains("soft"));
    }

    @Test
    void seniorityGate_enrichmentJuniorBoostsRank() {
        CanonicalJobEntity job = baseJob("Software Engineer", "Dublin, Ireland");
        job.setSeniority("junior");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        assertFalse(job.isHiddenByDefault());
        assertEquals("HIGH", job.getRankTier());
        assertTrue(job.getGateDimensionsJson().contains("JUNIOR_FIT"));
    }

    @Test
    void seniorityGate_allowsGraduateDespiteYearsMention() {
        CanonicalJobEntity job = baseJob("Graduate Software Engineer", "Dublin, Ireland");
        job.setDescription("Graduate programme. Mentors typically have 5 years of experience.");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        assertEquals("NEEDS_CONFIRMATION", job.getGateStatus());
        assertFalse(job.isHiddenByDefault());
    }

    @Test
    void seniorityGate_failsSeniorTitle() {
        CanonicalJobEntity job = baseJob("Senior Software Engineer", "Dublin, Ireland");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        assertEquals("FAILED", job.getGateStatus());
        assertTrue(job.isHiddenByDefault());
    }

    @Test
    void seniorityGate_failsSoftwareDevelopmentManager() {
        CanonicalJobEntity job = baseJob("Software Development Manager, Network Capacity Services", "Dublin, Ireland");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        assertEquals("FAILED", job.getGateStatus());
        assertTrue(job.isHiddenByDefault());
    }

    @Test
    void seniorityGate_failsLevelledEngineerIii() {
        CanonicalJobEntity job = baseJob("Software Engineer III - AI & IoT", "Dublin, Ireland");
        job.setExpectedStartDate(null);

        service.evaluate(job, new ProfileEntity());

        assertEquals("FAILED", job.getGateStatus());
        assertTrue(job.isHiddenByDefault());
    }

    private CanonicalJobEntity baseJob(String title, String location) {
        CanonicalJobEntity job = new CanonicalJobEntity();
        job.setJobId(UUID.randomUUID());
        job.setTitle(title);
        job.setLocation(location);
        job.setJobStatus("NEW");
        job.setValidityStatus("ACTIVE");
        job.setGateStatus("UNKNOWN");
        job.setRankTier("UNRANKED");
        job.setHiddenByDefault(false);
        job.setVersion(1);
        return job;
    }
}
