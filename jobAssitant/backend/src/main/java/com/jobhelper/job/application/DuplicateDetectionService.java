package com.jobhelper.job.application;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.job.infrastructure.CanonicalJobRepository;
import com.jobhelper.job.infrastructure.JobDecisionEntity;
import com.jobhelper.job.infrastructure.JobDecisionRepository;
import com.jobhelper.job.infrastructure.JobPossibleDuplicateEntity;
import com.jobhelper.job.infrastructure.JobPossibleDuplicateRepository;
import com.jobhelper.job.infrastructure.JobSourceRefEntity;
import com.jobhelper.job.infrastructure.JobSourceRefRepository;
import com.jobhelper.shared.web.ApiException;

/**
 * FR-JOB-007/008 — hard dedupe by canonical apply URL, fuzzy (company + similar title)
 * possible-duplicate detection, and CONFIRMED/REJECTED resolution per job-model §3.9.
 */
@Service
public class DuplicateDetectionService {

    private static final double TITLE_SIMILARITY_THRESHOLD = 0.6;
    private static final Set<String> URL_NOISE_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "ref", "referrer", "source", "trk", "trkid", "fbclid", "gclid", "gh_src");
    private static final Set<String> PROTECTED_STATUSES = Set.of("SHORTLISTED", "APPLIED");

    private final JobPossibleDuplicateRepository duplicateRepository;
    private final CanonicalJobRepository jobRepository;
    private final JobSourceRefRepository sourceRefRepository;
    private final JobDecisionRepository jobDecisionRepository;
    private final ObjectMapper objectMapper;

    public DuplicateDetectionService(
            JobPossibleDuplicateRepository duplicateRepository,
            CanonicalJobRepository jobRepository,
            JobSourceRefRepository sourceRefRepository,
            JobDecisionRepository jobDecisionRepository,
            ObjectMapper objectMapper) {
        this.duplicateRepository = duplicateRepository;
        this.jobRepository = jobRepository;
        this.sourceRefRepository = sourceRefRepository;
        this.jobDecisionRepository = jobDecisionRepository;
        this.objectMapper = objectMapper;
    }

    // ---- Hard dedupe (canonical apply URL) --------------------------------------------------

    public String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            String query = stripNoiseParams(uri.getRawQuery());
            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(host).append(path);
            if (!query.isBlank()) {
                sb.append('?').append(query);
            }
            return sb.toString();
        } catch (Exception ex) {
            return raw.trim().toLowerCase(Locale.ROOT);
        }
    }

    private String stripNoiseParams(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        List<String> keep = new ArrayList<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            String key = pair.split("=", 2)[0].toLowerCase(Locale.ROOT);
            if (URL_NOISE_PARAMS.contains(key)) {
                continue;
            }
            keep.add(pair);
        }
        Collections.sort(keep);
        return String.join("&", keep);
    }

    /** Finds an existing active job whose canonical apply URL normalizes to the same value. */
    @Transactional(readOnly = true)
    public Optional<CanonicalJobEntity> findHardDedupeMatchByUrl(String rawUrl, UUID excludeJobId) {
        String normalized = normalizeUrl(rawUrl);
        if (normalized == null) {
            return Optional.empty();
        }
        return jobRepository.findByCanonicalApplyUrlIsNotNull().stream()
                .filter(j -> excludeJobId == null || !j.getJobId().equals(excludeJobId))
                .filter(j -> !"ARCHIVED".equalsIgnoreCase(j.getJobStatus()))
                .filter(j -> normalized.equals(normalizeUrl(j.getCanonicalApplyUrl())))
                .findFirst();
    }

    // ---- Source refs --------------------------------------------------------------------------

    @Transactional
    public void upsertSourceRef(UUID jobId, String sourceCode, String sourceStableId, String applyUrl, boolean preferredIfNew) {
        Instant now = Instant.now();
        Optional<JobSourceRefEntity> existing = sourceStableId != null
                ? sourceRefRepository.findBySourceCodeAndSourceStableId(sourceCode, sourceStableId)
                : Optional.empty();
        if (existing.isPresent()) {
            JobSourceRefEntity ref = existing.get();
            ref.setJobId(jobId);
            ref.setApplyUrl(applyUrl);
            ref.setLastSeenAt(now);
            ref.setUpdatedAt(now);
            sourceRefRepository.save(ref);
            return;
        }
        JobSourceRefEntity ref = new JobSourceRefEntity();
        ref.setJobSourceRefId(UUID.randomUUID());
        ref.setJobId(jobId);
        ref.setSourceCode(sourceCode);
        ref.setSourceStableId(sourceStableId);
        ref.setApplyUrl(applyUrl);
        ref.setPreferred(preferredIfNew && sourceRefRepository.countByJobId(jobId) == 0);
        ref.setFirstSeenAt(now);
        ref.setLastSeenAt(now);
        ref.setCreatedAt(now);
        ref.setUpdatedAt(now);
        sourceRefRepository.save(ref);
    }

    // ---- Fuzzy detection (same company + similar title) --------------------------------------

    @Transactional
    public void detectFuzzyDuplicates(CanonicalJobEntity job) {
        if (job.getCompany() == null || job.getCompany().isBlank()
                || job.getTitle() == null || job.getTitle().isBlank()
                || "ARCHIVED".equalsIgnoreCase(job.getJobStatus())) {
            return;
        }
        for (CanonicalJobEntity other : jobRepository.findByCompanyIgnoreCase(job.getCompany())) {
            if (other.getJobId().equals(job.getJobId()) || "ARCHIVED".equalsIgnoreCase(other.getJobStatus())) {
                continue;
            }
            double similarity = titleSimilarity(job.getTitle(), other.getTitle());
            if (similarity >= TITLE_SIMILARITY_THRESHOLD) {
                createPendingDuplicateIfAbsent(job, other, similarity);
            }
        }
    }

    private void createPendingDuplicateIfAbsent(CanonicalJobEntity a, CanonicalJobEntity b, double similarity) {
        // PostgreSQL uuid `<` is unsigned byte order; Java UUID.compareTo is signed — must match PG
        // for chk_job_possible_duplicate_order (left_job_id < right_job_id).
        UUID leftId;
        UUID rightId;
        if (compareUuidUnsigned(a.getJobId(), b.getJobId()) < 0) {
            leftId = a.getJobId();
            rightId = b.getJobId();
        } else {
            leftId = b.getJobId();
            rightId = a.getJobId();
        }
        if (duplicateRepository.findByLeftJobIdAndRightJobId(leftId, rightId).isPresent()) {
            return; // never re-flag a pair already resolved (or pending) once recorded
        }
        Map<String, Object> signals = new HashMap<>();
        signals.put("detectionMethod", "FUZZY_COMPANY_TITLE");
        signals.put("titleSimilarity", Math.round(similarity * 1000.0) / 1000.0);
        signals.put("companyMatch", true);
        signals.put("leftTitle", leftId.equals(a.getJobId()) ? a.getTitle() : b.getTitle());
        signals.put("rightTitle", leftId.equals(a.getJobId()) ? b.getTitle() : a.getTitle());
        signals.put("company", a.getCompany());
        signals.put("detectedAt", Instant.now().toString());

        JobPossibleDuplicateEntity d = new JobPossibleDuplicateEntity();
        d.setDuplicateId(UUID.randomUUID());
        d.setLeftJobId(leftId);
        d.setRightJobId(rightId);
        d.setSignals(toJson(signals));
        d.setStatus("PENDING");
        Instant now = Instant.now();
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        duplicateRepository.save(d);

        refreshHasPendingDuplicateFlag(leftId);
        refreshHasPendingDuplicateFlag(rightId);
    }

    private double titleSimilarity(String a, String b) {
        Set<String> ta = tokenize(a);
        Set<String> tb = tokenize(b);
        if (ta.isEmpty() || tb.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(ta);
        intersection.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String s) {
        if (s == null) {
            return Set.of();
        }
        String normalized = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        return new HashSet<>(Arrays.asList(normalized.trim().split("\\s+")))
                .stream().filter(t -> t.length() > 1).collect(java.util.stream.Collectors.toSet());
    }

    // ---- Resolution: confirm / reject -----------------------------------------------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDuplicates(String status) {
        String normalized = (status == null || status.isBlank()) ? "PENDING" : status.toUpperCase(Locale.ROOT);
        return duplicateRepository.findByStatusOrderByCreatedAtDesc(normalized).stream()
                .map(this::duplicateDto).toList();
    }

    public Map<String, Object> duplicateDto(JobPossibleDuplicateEntity d) {
        Map<String, Object> m = new HashMap<>();
        m.put("duplicateId", d.getDuplicateId());
        m.put("status", d.getStatus());
        m.put("leftJob", jobSummary(d.getLeftJobId()));
        m.put("rightJob", jobSummary(d.getRightJobId()));
        m.put("signals", parseSignals(d.getSignals()));
        m.put("createdAt", d.getCreatedAt());
        return m;
    }

    private Map<String, Object> jobSummary(UUID jobId) {
        return jobRepository.findById(jobId).<Map<String, Object>>map(j -> {
            Map<String, Object> m = new HashMap<>();
            m.put("jobId", j.getJobId());
            m.put("title", j.getTitle());
            m.put("company", j.getCompany());
            m.put("location", j.getLocation());
            m.put("jobStatus", j.getJobStatus());
            m.put("lastSeenAt", j.getLastSeenAt());
            return m;
        }).orElseGet(() -> Map.of("jobId", jobId));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> pendingDuplicatesForJob(UUID jobId) {
        return duplicateRepository.findByStatusAndJobId("PENDING", jobId).stream().map(d -> {
            UUID otherJobId = d.getLeftJobId().equals(jobId) ? d.getRightJobId() : d.getLeftJobId();
            Map<String, Object> m = new HashMap<>();
            m.put("duplicateId", d.getDuplicateId());
            m.put("otherJobId", otherJobId);
            jobRepository.findById(otherJobId).ifPresent(other -> {
                m.put("otherJobTitle", other.getTitle());
                m.put("otherJobCompany", other.getCompany());
            });
            m.put("signals", parseSignals(d.getSignals()));
            m.put("status", d.getStatus());
            m.put("createdAt", d.getCreatedAt());
            return m;
        }).toList();
    }

    private Object parseSignals(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Object node = objectMapper.readTree(json);
            return node == null ? Map.of() : node;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            return "{}";
        }
    }

    @Transactional
    public Map<String, Object> confirm(UUID duplicateId, UUID survivorJobIdOverride, String resolvedJobStatus) {
        JobPossibleDuplicateEntity duplicate = duplicateRepository.findById(duplicateId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DUPLICATE_NOT_FOUND", "Possible duplicate not found"));
        if (!"PENDING".equals(duplicate.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_ALREADY_RESOLVED",
                    "Duplicate already resolved as " + duplicate.getStatus());
        }
        CanonicalJobEntity left = jobRepository.findById(duplicate.getLeftJobId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Job not found"));
        CanonicalJobEntity right = jobRepository.findById(duplicate.getRightJobId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Job not found"));

        boolean needsUserDecision = PROTECTED_STATUSES.contains(left.getJobStatus())
                || PROTECTED_STATUSES.contains(right.getJobStatus());
        if (needsUserDecision && (survivorJobIdOverride == null || resolvedJobStatus == null)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_MERGE_CONFLICT",
                    "Both survivorJobId and resolvedJobStatus are required when a job is SHORTLISTED/APPLIED",
                    Map.of(
                            "leftJobId", left.getJobId(), "leftJobStatus", left.getJobStatus(),
                            "rightJobId", right.getJobId(), "rightJobStatus", right.getJobStatus()));
        }

        CanonicalJobEntity survivor;
        CanonicalJobEntity archived;
        if (survivorJobIdOverride != null) {
            if (survivorJobIdOverride.equals(left.getJobId())) {
                survivor = left;
                archived = right;
            } else if (survivorJobIdOverride.equals(right.getJobId())) {
                survivor = right;
                archived = left;
            } else {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "survivorJobId must be one of the duplicate pair's job ids");
            }
        } else {
            // Default survivor rule (job-model §3.9): earlier first_seen_at, tie-break earlier created_at.
            survivor = earlierSeen(left, right);
            archived = survivor == left ? right : left;
        }

        Instant now = Instant.now();
        if (resolvedJobStatus != null) {
            String from = survivor.getJobStatus();
            if (!from.equals(resolvedJobStatus)) {
                survivor.setJobStatus(resolvedJobStatus);
                saveDecision(survivor.getJobId(), "STATUS_CHANGE", from, resolvedJobStatus, "duplicate merge resolution");
            }
        }

        for (JobSourceRefEntity ref : sourceRefRepository.findByJobIdOrderByLastSeenAtDesc(archived.getJobId())) {
            ref.setJobId(survivor.getJobId());
            ref.setPreferred(false);
            ref.setUpdatedAt(now);
            sourceRefRepository.save(ref);
        }

        archived.setJobStatus("ARCHIVED");
        archived.setHiddenByDefault(true);
        archived.setVersion(archived.getVersion() + 1);
        archived.setUpdatedAt(now);
        jobRepository.save(archived);

        survivor.setVersion(survivor.getVersion() + 1);
        survivor.setUpdatedAt(now);
        jobRepository.save(survivor);

        JobDecisionEntity decision = saveDecision(survivor.getJobId(), "DUPLICATE_CONFIRMATION",
                archived.getJobId().toString(), survivor.getJobId().toString(), "confirmed possible duplicate " + duplicateId);

        duplicate.setStatus("CONFIRMED");
        duplicate.setDecidedByDecisionId(decision.getDecisionId());
        duplicate.setUpdatedAt(now);
        duplicateRepository.save(duplicate);

        refreshHasPendingDuplicateFlag(survivor.getJobId());
        refreshHasPendingDuplicateFlag(archived.getJobId());

        return Map.of("survivorJobId", survivor.getJobId(), "archivedJobId", archived.getJobId());
    }

    private CanonicalJobEntity earlierSeen(CanonicalJobEntity a, CanonicalJobEntity b) {
        Instant aSeen = a.getFirstSeenAt();
        Instant bSeen = b.getFirstSeenAt();
        if (aSeen != null && bSeen != null && !aSeen.equals(bSeen)) {
            return aSeen.isBefore(bSeen) ? a : b;
        }
        if (aSeen != null && bSeen == null) {
            return a;
        }
        if (bSeen != null && aSeen == null) {
            return b;
        }
        Instant aCreated = a.getCreatedAt();
        Instant bCreated = b.getCreatedAt();
        if (aCreated != null && bCreated != null && !aCreated.equals(bCreated)) {
            return aCreated.isBefore(bCreated) ? a : b;
        }
        return a;
    }

    @Transactional
    public void reject(UUID duplicateId) {
        JobPossibleDuplicateEntity duplicate = duplicateRepository.findById(duplicateId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DUPLICATE_NOT_FOUND", "Possible duplicate not found"));
        if (!"PENDING".equals(duplicate.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_ALREADY_RESOLVED",
                    "Duplicate already resolved as " + duplicate.getStatus());
        }
        duplicate.setStatus("REJECTED");
        duplicate.setUpdatedAt(Instant.now());
        duplicateRepository.save(duplicate);
        refreshHasPendingDuplicateFlag(duplicate.getLeftJobId());
        refreshHasPendingDuplicateFlag(duplicate.getRightJobId());
    }

    private JobDecisionEntity saveDecision(UUID jobId, String type, String from, String to, String reason) {
        JobDecisionEntity d = new JobDecisionEntity();
        d.setDecisionId(UUID.randomUUID());
        d.setJobId(jobId);
        d.setDecisionType(type);
        d.setFromStatus(from);
        d.setToStatus(to);
        d.setReason(reason);
        d.setCreatedAt(Instant.now());
        jobDecisionRepository.save(d);
        return d;
    }

    private void refreshHasPendingDuplicateFlag(UUID jobId) {
        boolean pending = duplicateRepository.existsPendingForJob(jobId);
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.isHasPendingDuplicate() != pending) {
                job.setHasPendingDuplicate(pending);
                jobRepository.save(job);
            }
        });
    }

    /** Match PostgreSQL uuid ordering (unsigned 16-byte lexicographic). */
    static int compareUuidUnsigned(UUID a, UUID b) {
        long am = a.getMostSignificantBits();
        long bm = b.getMostSignificantBits();
        int c = Long.compareUnsigned(am, bm);
        if (c != 0) {
            return c;
        }
        return Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    }
}
