package com.jobhelper.profile.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import com.jobhelper.job.application.GateRankService;
import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.job.infrastructure.CanonicalJobRepository;
import com.jobhelper.profile.infrastructure.ProfileEntity;
import com.jobhelper.profile.infrastructure.ProfileRepository;
import com.jobhelper.profile.infrastructure.RecomputeRequestEntity;
import com.jobhelper.profile.infrastructure.RecomputeRequestRepository;
import com.jobhelper.roadmap.application.RoadmapService;
import com.jobhelper.roadmap.infrastructure.RoadmapRepository;
import com.jobhelper.shared.web.ApiException;

@Service
public class RecomputeRequestService {
    private static final List<String> OPEN_STATUSES = List.of("PENDING", "DEFERRED");
    private static final List<String> DEFAULT_SCOPES = List.of("ROADMAP", "JOB_GATE", "JOB_RANK");

    private final RecomputeRequestRepository recomputeRequestRepository;
    private final ProfileRepository profileRepository;
    private final RoadmapRepository roadmapRepository;
    private final RoadmapService roadmapService;
    private final GateRankService gateRankService;
    private final CanonicalJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public RecomputeRequestService(
            RecomputeRequestRepository recomputeRequestRepository,
            ProfileRepository profileRepository,
            RoadmapRepository roadmapRepository,
            RoadmapService roadmapService,
            GateRankService gateRankService,
            CanonicalJobRepository jobRepository,
            ObjectMapper objectMapper) {
        this.recomputeRequestRepository = recomputeRequestRepository;
        this.profileRepository = profileRepository;
        this.roadmapRepository = roadmapRepository;
        this.roadmapService = roadmapService;
        this.gateRankService = gateRankService;
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RecomputeRequestEntity createPending(int profileVersion, List<String> changedFields, boolean requiresRoadmapPreview) {
        supersedeOpen();
        Instant now = Instant.now();
        RecomputeRequestEntity req = new RecomputeRequestEntity();
        req.setRecomputeRequestId(UUID.randomUUID());
        req.setProfileVersion(profileVersion);
        req.setStatus("PENDING");
        req.setScopes(String.join(",", DEFAULT_SCOPES));
        req.setChangeSummary(String.join(",", changedFields));
        req.setImpactSummary("{\"affectedScopes\":[\"ROADMAP\",\"JOB_GATE\",\"JOB_RANK\"],\"requiresRoadmapPreview\":"
                + requiresRoadmapPreview + "}");
        req.setCreatedAt(now);
        req.setUpdatedAt(now);
        return recomputeRequestRepository.save(req);
    }

    @Transactional
    public void supersedeOpen() {
        Instant now = Instant.now();
        for (RecomputeRequestEntity open : recomputeRequestRepository.findByStatusIn(OPEN_STATUSES)) {
            open.setStatus("SUPERSEDED");
            open.setUpdatedAt(now);
            recomputeRequestRepository.save(open);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> current() {
        return recomputeRequestRepository.findFirstByStatusInOrderByCreatedAtDesc(OPEN_STATUSES)
                .map(this::toDto)
                .orElse(Map.of("status", "NONE"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(UUID id) {
        return toDto(load(id));
    }

    @Transactional
    public Map<String, Object> preview(UUID id, List<String> selectedScopes) {
        RecomputeRequestEntity req = loadOpen(id);
        List<String> scopes = normalizeScopes(selectedScopes);
        Map<String, Object> response = new HashMap<>();
        response.put("recomputeRequestId", req.getRecomputeRequestId());
        response.put("profileVersion", req.getProfileVersion());
        response.put("selectedScopes", scopes);
        response.put("willNotOverwrite", List.of("JobDecision", "UserRoadmapTasks", "CompletedTasks"));

        if (scopes.contains("ROADMAP")) {
            Map<String, Object> roadmapPreview = roadmapService.buildRecomputePreview();
            String token = String.valueOf(roadmapPreview.get("previewToken"));
            req.setPreviewToken(token);
            req.setPreviewExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            try {
                req.setPreviewPayload(objectMapper.writeValueAsString(Map.of(
                        "selectedScopes", scopes,
                        "roadmapPreview", roadmapPreview)));
            } catch (Exception e) {
                req.setPreviewPayload("{\"selectedScopes\":" + scopes + "}");
            }
            req.setUpdatedAt(Instant.now());
            recomputeRequestRepository.save(req);
            response.put("roadmapPreview", roadmapPreview);
            response.put("roadmapPreviewToken", token);
        } else {
            response.put("roadmapPreview", null);
            response.put("roadmapPreviewToken", null);
        }

        if (scopes.contains("JOB_GATE") || scopes.contains("JOB_RANK")) {
            long jobs = jobRepository.count();
            Map<String, Object> gatePreview = Map.of(
                    "jobsAffected", jobs,
                    "statusChangesSummary", List.of("re-evaluate all non-protected jobs"));
            if (scopes.contains("JOB_GATE")) {
                response.put("jobGatePreview", gatePreview);
            }
            if (scopes.contains("JOB_RANK")) {
                response.put("jobRankPreview", gatePreview);
            }
        }
        return response;
    }

    @Transactional
    public Map<String, Object> defer(UUID id) {
        RecomputeRequestEntity req = loadOpen(id);
        req.setStatus("DEFERRED");
        req.setUpdatedAt(Instant.now());
        recomputeRequestRepository.save(req);
        return Map.of("status", "DEFERRED");
    }

    @Transactional
    public Map<String, Object> confirm(UUID id, List<String> selectedScopes, boolean confirmRoadmapPreview, String roadmapPreviewToken) {
        RecomputeRequestEntity req = loadOpen(id);
        ProfileEntity profile = profileRepository.findSingleton()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile required"));
        if (req.getProfileVersion() != profile.getProfileVersion()) {
            req.setStatus("SUPERSEDED");
            req.setUpdatedAt(Instant.now());
            recomputeRequestRepository.save(req);
            throw new ApiException(HttpStatus.CONFLICT, "RECOMPUTE_SUPERSEDED", "Recompute request superseded by newer profile");
        }

        List<String> scopes = normalizeScopes(selectedScopes);
        if (scopes.contains("ROADMAP")) {
            if (!confirmRoadmapPreview) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ROADMAP_PREVIEW_REQUIRED", "confirmRoadmapPreview required");
            }
            String token = roadmapPreviewToken != null ? roadmapPreviewToken : req.getPreviewToken();
            if (token == null || token.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ROADMAP_PREVIEW_REQUIRED", "roadmapPreviewToken required");
            }
            if (req.getPreviewExpiresAt() != null && Instant.now().isAfter(req.getPreviewExpiresAt())) {
                throw new ApiException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED", "Roadmap preview token expired");
            }
            if (req.getPreviewToken() != null && !req.getPreviewToken().equals(token)) {
                throw new ApiException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED", "Roadmap preview token mismatch");
            }
        }

        req.setStatus("RUNNING");
        req.setUpdatedAt(Instant.now());
        recomputeRequestRepository.save(req);

        Map<String, Object> resultSummary = new HashMap<>();
        List<String> failed = new ArrayList<>();

        if (scopes.contains("ROADMAP")) {
            try {
                String token = roadmapPreviewToken != null ? roadmapPreviewToken : req.getPreviewToken();
                Map<String, Object> merge = roadmapService.mergeOrGenerate(token);
                resultSummary.put("ROADMAP", merge.getOrDefault("resultSummary", merge));
            } catch (Exception ex) {
                failed.add("ROADMAP");
                resultSummary.put("ROADMAP", Map.of("error", ex.getMessage()));
            }
        }
        if (scopes.contains("JOB_GATE") || scopes.contains("JOB_RANK")) {
            try {
                int count = reevaluateJobs(profile);
                Map<String, Object> jobResult = Map.of("recomputedJobCount", count);
                if (scopes.contains("JOB_GATE")) {
                    resultSummary.put("JOB_GATE", jobResult);
                }
                if (scopes.contains("JOB_RANK")) {
                    resultSummary.put("JOB_RANK", jobResult);
                }
            } catch (Exception ex) {
                if (scopes.contains("JOB_GATE")) {
                    failed.add("JOB_GATE");
                }
                if (scopes.contains("JOB_RANK")) {
                    failed.add("JOB_RANK");
                }
                resultSummary.put("JOB", Map.of("error", ex.getMessage()));
            }
        }

        String status;
        if (failed.isEmpty()) {
            status = "COMPLETED";
        } else if (failed.size() == scopes.size()) {
            status = "FAILED";
        } else {
            status = "PARTIALLY_FAILED";
        }
        req.setStatus(status);
        req.setConfirmedAt(Instant.now());
        req.setUpdatedAt(Instant.now());
        try {
            req.setImpactSummary(objectMapper.writeValueAsString(resultSummary));
        } catch (Exception ignored) {
            // keep previous
        }
        recomputeRequestRepository.save(req);
        return Map.of("status", status, "resultSummary", resultSummary);
    }

    public boolean requiresRoadmapPreview(ProfileEntity before, ProfileEntity after, boolean created) {
        if (created) {
            return true;
        }
        if (roadmapRepository.findFirstByStatusOrderByUpdatedAtDesc("ACTIVE").isEmpty()) {
            return true;
        }
        if (before == null) {
            return true;
        }
        return !graduationKey(before).equals(graduationKey(after))
                || !stampKey(before).equals(stampKey(after));
    }

    private int reevaluateJobs(ProfileEntity profile) {
        int count = 0;
        for (CanonicalJobEntity job : jobRepository.findAll()) {
            if (isProtected(job.getJobStatus())) {
                continue;
            }
            gateRankService.evaluate(job, profile);
            job.setVersion(job.getVersion() + 1);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            count++;
        }
        return count;
    }

    private boolean isProtected(String status) {
        return "SHORTLISTED".equals(status) || "APPLIED".equals(status) || "REJECTED".equals(status);
    }

    private RecomputeRequestEntity load(UUID id) {
        return recomputeRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECOMPUTE_NOT_FOUND", "Recompute request not found"));
    }

    private RecomputeRequestEntity loadOpen(UUID id) {
        RecomputeRequestEntity req = load(id);
        if (!OPEN_STATUSES.contains(req.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "RECOMPUTE_SUPERSEDED",
                    "Recompute request is " + req.getStatus());
        }
        return req;
    }

    private List<String> normalizeScopes(List<String> selected) {
        if (selected == null || selected.isEmpty()) {
            return new ArrayList<>(DEFAULT_SCOPES);
        }
        return selected.stream().filter(DEFAULT_SCOPES::contains).distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Object> toDto(RecomputeRequestEntity req) {
        Map<String, Object> m = new HashMap<>();
        m.put("recomputeRequestId", req.getRecomputeRequestId());
        m.put("profileVersion", req.getProfileVersion());
        m.put("status", req.getStatus());
        m.put("scopes", List.of(req.getScopes().split(",")));
        m.put("changeSummary", req.getChangeSummary());
        m.put("impactSummary", req.getImpactSummary());
        m.put("previewToken", req.getPreviewToken());
        m.put("previewExpiresAt", req.getPreviewExpiresAt());
        m.put("createdAt", req.getCreatedAt());
        m.put("updatedAt", req.getUpdatedAt());
        m.put("confirmedAt", req.getConfirmedAt());
        return m;
    }

    private String graduationKey(ProfileEntity p) {
        return p.getEducationPeriods().stream()
                .map(e -> String.valueOf(e.getExpectedGraduationDate()))
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private String stampKey(ProfileEntity p) {
        return p.getWorkAuthorizations().stream()
                .map(w -> String.valueOf(w.getValidUntil()))
                .sorted()
                .collect(Collectors.joining("|"));
    }
}
