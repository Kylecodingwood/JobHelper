package com.jobhelper.job.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jobhelper.job.application.DuplicateDetectionService;
import com.jobhelper.job.application.GateRankService;
import com.jobhelper.job.application.JobSyncService;
import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.job.infrastructure.CanonicalJobRepository;
import com.jobhelper.job.infrastructure.CanonicalJobSpecifications;
import com.jobhelper.job.infrastructure.JobDecisionEntity;
import com.jobhelper.job.infrastructure.JobDecisionRepository;
import com.jobhelper.job.infrastructure.JobSourceRefEntity;
import com.jobhelper.job.infrastructure.JobSourceRefRepository;
import com.jobhelper.profile.infrastructure.ProfileRepository;
import com.jobhelper.shared.outbox.OutboxService;
import com.jobhelper.shared.web.ApiException;

@RestController
@RequestMapping("/api/v1")
public class JobController {
    private final CanonicalJobRepository jobRepository;
    private final JobSyncService jobSyncService;
    private final GateRankService gateRankService;
    private final ProfileRepository profileRepository;
    private final JobDecisionRepository jobDecisionRepository;
    private final JobSourceRefRepository jobSourceRefRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public JobController(
            CanonicalJobRepository jobRepository,
            JobSyncService jobSyncService,
            GateRankService gateRankService,
            ProfileRepository profileRepository,
            JobDecisionRepository jobDecisionRepository,
            JobSourceRefRepository jobSourceRefRepository,
            DuplicateDetectionService duplicateDetectionService,
            OutboxService outboxService,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.jobSyncService = jobSyncService;
        this.gateRankService = gateRankService;
        this.profileRepository = profileRepository;
        this.jobDecisionRepository = jobDecisionRepository;
        this.jobSourceRefRepository = jobSourceRefRepository;
        this.duplicateDetectionService = duplicateDetectionService;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/jobs")
    public Map<String, Object> list(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> rankTier,
            @RequestParam(required = false) List<String> gateStatus,
            @RequestParam(required = false) List<String> validityStatus,
            @RequestParam(defaultValue = "false") boolean includeHidden,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) Boolean hasPendingDuplicate,
            @RequestParam(required = false) String sourceCode,
            @RequestParam(required = false) String q,
            /** Cap band: junior (default via FE) | mid | all/blank */
            @RequestParam(required = false) String fit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastSeenAt,desc") String sort) {
        Specification<CanonicalJobEntity> spec = CanonicalJobSpecifications.filter(
                status, rankTier, gateStatus, validityStatus, includeHidden, includeArchived,
                hasPendingDuplicate, sourceCode, q, fit);
        Page<CanonicalJobEntity> result = jobRepository.findAll(spec, PageRequest.of(page, size, parseSort(sort)));
        return Map.of(
                "content", result.getContent().stream().map(this::summary).toList(),
                "totalElements", result.getTotalElements());
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "lastSeenAt");
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    @GetMapping("/jobs/new")
    public List<Map<String, Object>> listNew(@RequestParam(defaultValue = "10") int limit) {
        return jobRepository
                .findByJobStatusAndHiddenByDefaultFalseOrderByLastSeenAtDesc("NEW", PageRequest.of(0, limit))
                .stream().map(this::summary).toList();
    }

    @GetMapping("/jobs/{jobId}")
    public Map<String, Object> detail(@PathVariable UUID jobId) {
        CanonicalJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Job not found"));
        Map<String, Object> detail = new HashMap<>(summary(job));
        detail.put("description", job.getDescription());
        detail.put("canonicalApplyUrl", job.getCanonicalApplyUrl());
        detail.put("seniority", job.getSeniority());
        detail.put("gateDimensions", parseList(job.getGateDimensionsJson()));
        detail.put("rankFactors", parseList(job.getRankFactorsJson()));
        detail.put("pendingDuplicates", duplicateDetectionService.pendingDuplicatesForJob(jobId));
        detail.put("decisions", decisions(jobId));
        detail.put("sources", sources(jobId));
        return detail;
    }

    public record StatusChangeRequest(String toStatus, Integer expectedVersion) {}

    @PatchMapping("/jobs/{jobId}/status")
    public Map<String, Object> changeStatus(@PathVariable UUID jobId, @RequestBody StatusChangeRequest request) {
        CanonicalJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Job not found"));
        if (request.expectedVersion() == null || !request.expectedVersion().equals(job.getVersion())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOB_VERSION_CONFLICT", "Job version mismatch");
        }
        String from = job.getJobStatus();
        job.setJobStatus(request.toStatus());
        job.setVersion(job.getVersion() + 1);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        saveDecision(jobId, "STATUS_CHANGE", from, request.toStatus(), null);
        return detail(jobId);
    }

    public record GateOverrideRequest(String reason, Integer expectedVersion) {}

    @PostMapping("/jobs/{jobId}/gate-override")
    public Map<String, Object> gateOverride(@PathVariable UUID jobId, @RequestBody GateOverrideRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new ApiException(HttpStatus.valueOf(422), "GATE_OVERRIDE_REASON_REQUIRED", "reason required");
        }
        CanonicalJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Job not found"));
        if (request.expectedVersion() == null || !request.expectedVersion().equals(job.getVersion())) {
            throw new ApiException(HttpStatus.CONFLICT, "JOB_VERSION_CONFLICT", "Job version mismatch");
        }
        String from = job.getGateStatus();
        job.setGateStatus("PASSED");
        job.setHiddenByDefault(false);
        job.setVersion(job.getVersion() + 1);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        saveDecision(jobId, "GATE_OVERRIDE", from, "PASSED", request.reason());
        return detail(jobId);
    }

    @GetMapping("/jobs/{jobId}/evidence")
    public List<Map<String, Object>> evidence(
            @PathVariable UUID jobId,
            @RequestParam(required = false) String evaluationType) {
        CanonicalJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Job not found"));
        List<Map<String, Object>> items = new ArrayList<>();
        String type = evaluationType == null ? "" : evaluationType.toUpperCase();
        if (type.isBlank() || "GATE".equals(type)) {
            Object dims = parseList(job.getGateDimensionsJson());
            items.add(Map.of(
                    "evaluationType", "GATE",
                    "gateStatus", job.getGateStatus(),
                    "dimensions", dims));
        }
        if (type.isBlank() || "RANK".equals(type)) {
            Object factors = parseList(job.getRankFactorsJson());
            items.add(Map.of(
                    "evaluationType", "RANK",
                    "rankTier", job.getRankTier(),
                    "factors", factors));
        }
        if (type.isBlank() || "VALIDITY".equals(type)) {
            items.add(Map.of(
                    "evaluationType", "VALIDITY",
                    "validityStatus", job.getValidityStatus(),
                    "lastSeenAt", job.getLastSeenAt() == null ? "" : job.getLastSeenAt().toString()));
        }
        return items;
    }

    @GetMapping("/jobs/{jobId}/decisions")
    public List<Map<String, Object>> listDecisions(@PathVariable UUID jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Job not found");
        }
        return decisions(jobId);
    }

    @PostMapping("/jobs/{jobId}/validity/check")
    public Map<String, Object> validityCheck(@PathVariable UUID jobId) {
        CanonicalJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Job not found"));
        String previous = job.getValidityStatus();
        boolean stale = job.getLastSeenAt() != null
                && job.getLastSeenAt().isBefore(Instant.now().minusSeconds(60L * 60 * 24 * 30));
        job.setValidityStatus(stale ? "STALE" : "ACTIVE");
        job.setUpdatedAt(Instant.now());
        job.setVersion(job.getVersion() + 1);
        jobRepository.save(job);
        saveDecision(jobId, "VALIDITY_CHECK", previous, job.getValidityStatus(), stale ? "lastSeenAt > 30d" : "ok");
        return Map.of(
                "jobId", jobId,
                "validityStatus", job.getValidityStatus(),
                "previous", previous,
                "checkedAt", Instant.now().toString());
    }

    public record ManualUrlRequest(String url, String userProvidedJd, String note) {}

    @PostMapping("/jobs/manual-url")
    public ResponseEntity<Map<String, Object>> manualUrl(@RequestBody ManualUrlRequest request) {
        if (request.url() == null || request.url().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "url required");
        }
        var existing = duplicateDetectionService.findHardDedupeMatchByUrl(request.url(), null);
        if (existing.isPresent()) {
            return ResponseEntity.ok(detail(existing.get().getJobId()));
        }

        CanonicalJobEntity job = new CanonicalJobEntity();
        job.setJobId(UUID.randomUUID());
        job.setTitle("Manual import");
        job.setCompany("Unknown");
        job.setLocation("Ireland");
        job.setJobStatus("NEW");
        job.setValidityStatus("ACTIVE");
        job.setGateStatus("UNKNOWN");
        job.setRankTier("UNRANKED");
        job.setHiddenByDefault(false);
        Instant now = Instant.now();
        job.setLastSeenAt(now);
        job.setFirstSeenAt(now);
        job.setPreferredSourceCode("manual_url");
        job.setSourceStableId(request.url());
        job.setHasPendingDuplicate(false);
        job.setVersion(1);
        job.setDescription(request.userProvidedJd());
        job.setCanonicalApplyUrl(request.url());
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        profileRepository.findSingleton().ifPresentOrElse(
                p -> gateRankService.evaluate(job, p),
                () -> {
                    job.setGateStatus("NEEDS_CONFIRMATION");
                    job.setRankTier("UNRANKED");
                });
        jobRepository.save(job);
        duplicateDetectionService.upsertSourceRef(job.getJobId(), "manual_url", request.url(), request.url(), true);
        duplicateDetectionService.detectFuzzyDuplicates(job);
        outboxService.append("JOB", "JOB_CREATED", "JOB", job.getJobId(),
                "{\"title\":\"" + job.getTitle().replace("\"", "'") + "\"}");
        return ResponseEntity.status(HttpStatus.CREATED).body(detail(job.getJobId()));
    }

    @GetMapping("/jobs/duplicates")
    public List<Map<String, Object>> listDuplicates(@RequestParam(defaultValue = "PENDING") String status) {
        return duplicateDetectionService.listDuplicates(status);
    }

    public record DuplicateConfirmRequest(UUID survivorJobId, String resolvedJobStatus) {}

    @PostMapping("/jobs/duplicates/{duplicateId}/confirm")
    public Map<String, Object> confirmDuplicate(
            @PathVariable UUID duplicateId,
            @RequestBody(required = false) DuplicateConfirmRequest request) {
        UUID survivorJobId = request == null ? null : request.survivorJobId();
        String resolvedJobStatus = request == null ? null : request.resolvedJobStatus();
        return duplicateDetectionService.confirm(duplicateId, survivorJobId, resolvedJobStatus);
    }

    @PostMapping("/jobs/duplicates/{duplicateId}/reject")
    public ResponseEntity<Void> rejectDuplicate(@PathVariable UUID duplicateId) {
        duplicateDetectionService.reject(duplicateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/job-sources")
    public List<Map<String, Object>> sources() {
        return jobSyncService.listSources();
    }

    @GetMapping("/job-sources/search-terms")
    public Map<String, Object> getSearchTerms() {
        return jobSyncService.getGlobalSearchTerms();
    }

    public record SearchTermsRequest(Object searchTerms) {}

    @PutMapping("/job-sources/search-terms")
    public Map<String, Object> putSearchTerms(@RequestBody SearchTermsRequest body) {
        if (body == null || body.searchTerms() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "searchTerms required");
        }
        return jobSyncService.putGlobalSearchTerms(body.searchTerms());
    }

    @GetMapping("/job-sources/{sourceId}")
    public Map<String, Object> source(@PathVariable UUID sourceId) {
        return jobSyncService.getSource(sourceId);
    }

    public record SourcePatchRequest(Boolean enabled, Object searchTerms, Object searchTermsOverride) {}

    @PatchMapping("/job-sources/{id}")
    public Map<String, Object> patchSource(@PathVariable("id") UUID sourceId, @RequestBody SourcePatchRequest body) {
        Object terms = body.searchTerms() != null ? body.searchTerms() : body.searchTermsOverride();
        return jobSyncService.patchSource(sourceId, body.enabled(), terms);
    }

    @GetMapping("/job-source-runs")
    public Map<String, Object> runs() {
        return jobSyncService.listRuns();
    }

    @GetMapping("/job-source-runs/{runId}")
    public Map<String, Object> run(@PathVariable UUID runId) {
        return jobSyncService.getRun(runId);
    }

    public record SyncRequest(UUID sourceId, List<String> siteScope, Boolean reuseLastParameters) {}

    @PostMapping("/job-source-runs")
    public Map<String, Object> startRun(@RequestBody SyncRequest request) {
        if (request.sourceId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "sourceId required");
        }
        return jobSyncService.runSync(
                request.sourceId(),
                "MANUAL",
                request.siteScope(),
                Boolean.TRUE.equals(request.reuseLastParameters()));
    }

    private List<Map<String, Object>> decisions(UUID jobId) {
        return jobDecisionRepository.findByJobIdOrderByCreatedAtDesc(jobId).stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("decisionId", d.getDecisionId());
                    m.put("decisionType", d.getDecisionType());
                    m.put("fromStatus", d.getFromStatus());
                    m.put("toStatus", d.getToStatus());
                    m.put("reason", d.getReason());
                    m.put("createdAt", d.getCreatedAt());
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> sources(UUID jobId) {
        return jobSourceRefRepository.findByJobIdOrderByLastSeenAtDesc(jobId).stream()
                .map(this::sourceRefDto)
                .toList();
    }

    private Map<String, Object> sourceRefDto(JobSourceRefEntity ref) {
        Map<String, Object> m = new HashMap<>();
        m.put("jobSourceRefId", ref.getJobSourceRefId());
        m.put("sourceCode", ref.getSourceCode());
        m.put("sourceStableId", ref.getSourceStableId());
        m.put("applyUrl", ref.getApplyUrl());
        m.put("isPreferred", ref.isPreferred());
        m.put("firstSeenAt", ref.getFirstSeenAt());
        m.put("lastSeenAt", ref.getLastSeenAt());
        return m;
    }

    private void saveDecision(UUID jobId, String type, String from, String to, String reason) {
        JobDecisionEntity d = new JobDecisionEntity();
        d.setDecisionId(UUID.randomUUID());
        d.setJobId(jobId);
        d.setDecisionType(type);
        d.setFromStatus(from);
        d.setToStatus(to);
        d.setReason(reason);
        d.setCreatedAt(Instant.now());
        jobDecisionRepository.save(d);
    }

    private Map<String, Object> summary(CanonicalJobEntity j) {
        Map<String, Object> m = new HashMap<>();
        m.put("jobId", j.getJobId());
        m.put("title", j.getTitle());
        m.put("company", j.getCompany());
        m.put("location", j.getLocation());
        m.put("jobStatus", j.getJobStatus());
        m.put("validityStatus", j.getValidityStatus());
        m.put("gateStatus", j.getGateStatus());
        m.put("rankTier", j.getRankTier());
        m.put("seniority", j.getSeniority());
        m.put("hiddenByDefault", j.isHiddenByDefault());
        m.put("expectedStartDate", j.getExpectedStartDate());
        m.put("lastSeenAt", j.getLastSeenAt());
        m.put("hasPendingDuplicate", j.isHasPendingDuplicate());
        m.put("preferredSourceCode", j.getPreferredSourceCode());
        m.put("version", j.getVersion());
        return m;
    }

    private Object parseList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return node == null ? List.of() : node;
        } catch (Exception e) {
            return List.of();
        }
    }
}
