package com.jobhelper.job.application;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.job.infrastructure.CanonicalJobRepository;
import com.jobhelper.job.infrastructure.JobSearchSettingsEntity;
import com.jobhelper.job.infrastructure.JobSearchSettingsRepository;
import com.jobhelper.job.infrastructure.JobSourceDiagnosticEntity;
import com.jobhelper.job.infrastructure.JobSourceDiagnosticRepository;
import com.jobhelper.job.infrastructure.JobSourceEntity;
import com.jobhelper.job.infrastructure.JobSourceRepository;
import com.jobhelper.job.infrastructure.JobSourceRunEntity;
import com.jobhelper.job.infrastructure.JobSourceRunRepository;
import com.jobhelper.profile.infrastructure.ProfileEntity;
import com.jobhelper.profile.infrastructure.ProfileRepository;
import com.jobhelper.shared.outbox.OutboxService;
import com.jobhelper.shared.web.ApiException;

@Service
public class JobSyncService {
    private static final Logger log = LoggerFactory.getLogger(JobSyncService.class);
    private static final short SEARCH_SETTINGS_ID = 1;

    private final JobSourceRepository sourceRepository;
    private final JobSourceRunRepository runRepository;
    private final JobSourceDiagnosticRepository diagnosticRepository;
    private final JobSearchSettingsRepository searchSettingsRepository;
    private final CanonicalJobRepository jobRepository;
    private final ProfileRepository profileRepository;
    private final GateRankService gateRankService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final DuplicateDetectionService duplicateDetectionService;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    @Value("${jobhelper.sync.freehire-limit:100}")
    private int freehireLimit;

    @Value("${jobhelper.sync.freehire-max-pages:3}")
    private int freehireMaxPages;

    @Value("${jobhelper.jobspy.script:}")
    private String jobspyScriptProperty;

    @Value("${jobhelper.jobspy.timeout-seconds:300}")
    private long jobspyTimeoutSeconds;

    @Value("${jobhelper.jobspy.results-wanted:50}")
    private int jobspyResultsWanted;

    public JobSyncService(
            JobSourceRepository sourceRepository,
            JobSourceRunRepository runRepository,
            JobSourceDiagnosticRepository diagnosticRepository,
            JobSearchSettingsRepository searchSettingsRepository,
            CanonicalJobRepository jobRepository,
            ProfileRepository profileRepository,
            GateRankService gateRankService,
            OutboxService outboxService,
            ObjectMapper objectMapper,
            DuplicateDetectionService duplicateDetectionService) {
        this.sourceRepository = sourceRepository;
        this.runRepository = runRepository;
        this.diagnosticRepository = diagnosticRepository;
        this.searchSettingsRepository = searchSettingsRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.gateRankService = gateRankService;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        this.duplicateDetectionService = duplicateDetectionService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSources() {
        List<String> terms = resolveSearchTerms();
        return sourceRepository.findAll().stream().map(s -> sourceDto(s, terms)).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSource(UUID sourceId) {
        JobSourceEntity source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOURCE_NOT_FOUND", "Source not found"));
        return sourceDto(source, resolveSearchTerms());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGlobalSearchTerms() {
        return Map.of("searchTerms", resolveSearchTerms());
    }

    @Transactional
    public Map<String, Object> putGlobalSearchTerms(Object searchTermsBody) {
        List<String> terms = normalizeSearchTerms(searchTermsBody);
        JobSearchSettingsEntity settings = searchSettingsRepository.findById(SEARCH_SETTINGS_ID)
                .orElseGet(() -> {
                    JobSearchSettingsEntity created = new JobSearchSettingsEntity();
                    created.setId(SEARCH_SETTINGS_ID);
                    return created;
                });
        settings.setSearchTerms(String.join(",", terms));
        settings.setUpdatedAt(Instant.now());
        searchSettingsRepository.save(settings);
        return Map.of("searchTerms", terms);
    }

    @Transactional
    public Map<String, Object> patchSource(UUID sourceId, Boolean enabled, Object searchTermsOverride) {
        JobSourceEntity source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOURCE_NOT_FOUND", "Source not found"));
        if (enabled != null) {
            source.setEnabled(enabled);
            source.setConfigVersion(source.getConfigVersion() + 1);
            source.setUpdatedAt(Instant.now());
            sourceRepository.save(source);
        }
        // Backward compatible: writing searchTerms on any source updates the global set.
        if (searchTermsOverride != null) {
            putGlobalSearchTerms(searchTermsOverride);
        }
        return sourceDto(source, resolveSearchTerms());
    }

    @Transactional
    public Map<String, Object> runSync(UUID sourceId) {
        return runSync(sourceId, "MANUAL", null, false);
    }

    @Transactional
    public Map<String, Object> runSync(UUID sourceId, String triggerType) {
        return runSync(sourceId, triggerType, null, false);
    }

    /**
     * @param siteScope optional subset of JobSpy sites to query (job-api §3 `POST /job-source-runs`);
     *                   ignored for non-JOBSPY adapters (applied partially, per design note).
     * @param reuseLastParameters if true, reuse the search terms captured on this source's most
     *                            recent run instead of recomputing from the current Profile/override
     *                            (applied only when a prior snapshot exists; falls back silently otherwise).
     */
    @Transactional
    public Map<String, Object> runSync(UUID sourceId, String triggerType, List<String> siteScope, boolean reuseLastParameters) {
        JobSourceEntity source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOURCE_NOT_FOUND", "Source not found"));
        if ("MANUAL".equals(source.getAdapterType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "manual_url does not sync");
        }
        if (!source.isEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SOURCE_DISABLED", "Source disabled");
        }
        if (runRepository.existsBySourceIdAndStatus(sourceId, "RUNNING")
                || runRepository.existsBySourceIdAndStatus(sourceId, "QUEUED")) {
            throw new ApiException(HttpStatus.CONFLICT, "SOURCE_RUN_ALREADY_ACTIVE",
                    "A sync run is already active for this source");
        }
        requireSearchTerms();

        List<String> terms = reuseLastParameters
                ? lastRunSearchTerms(sourceId).orElseGet(this::resolveSearchTerms)
                : resolveSearchTerms();
        if (terms.isEmpty()) {
            requireSearchTerms();
        }
        ProfileEntity profile = profileRepository.findSingleton().orElse(null);
        List<String> effectiveSiteScope = (siteScope == null) ? List.of()
                : siteScope.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList();

        Instant now = Instant.now();
        JobSourceRunEntity run = new JobSourceRunEntity();
        run.setRunId(UUID.randomUUID());
        run.setSourceId(source.getSourceId());
        run.setTriggerType(triggerType);
        run.setStatus("RUNNING");
        run.setParametersSnapshot(buildParametersSnapshot(terms, effectiveSiteScope, reuseLastParameters));
        run.setCreatedAt(now);
        run.setStartedAt(now);
        runRepository.save(run);

        List<String> diagnostics = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int received = 0;
        int valid = 0;
        int failed = 0;
        int requests = 0;

        try {
            if ("FREEHIRE_API".equals(source.getAdapterType())) {
                FreeHireFetchResult fh = fetchFreeHireDiscovery(terms, diagnostics, run.getRunId());
                requests += fh.requests();
                received += fh.jobs().size();
                diagnostics.add("FREEHIRE_QUERIES=" + fh.queryLabels());
                for (RawJob raw : fh.jobs()) {
                    if (raw.title() == null || raw.title().isBlank()) {
                        failed++;
                        addDiagnostic(run.getRunId(), "INVALID_ITEM", null, "missing title", raw.stableId());
                        continue;
                    }
                    valid++;
                    UpsertResult r = upsert(raw, profile, "freehire");
                    if (r.created()) {
                        created++;
                    } else {
                        updated++;
                    }
                }
            } else if ("JOBSPY".equals(source.getAdapterType())) {
                requests++;
                List<String> spyTerms = expandJobSpyTerms(terms);
                JobSpyResult spy = fetchJobSpy(spyTerms, diagnostics, run.getRunId(), effectiveSiteScope);
                received += spy.jobs().size();
                for (RawJob raw : spy.jobs()) {
                    if (raw.title() == null || raw.title().isBlank()) {
                        failed++;
                        addDiagnostic(run.getRunId(), "INVALID_ITEM", raw.site(), "missing title", raw.stableId());
                        continue;
                    }
                    valid++;
                    UpsertResult r = upsert(raw, profile, "jobspy");
                    if (r.created()) {
                        created++;
                    } else {
                        updated++;
                    }
                }
            }
            run.setStatus(failed > 0 && valid > 0 ? "PARTIAL_SUCCESS" : valid > 0 ? "SUCCEEDED" : "FAILED");
        } catch (Exception ex) {
            diagnostics.add("ERROR:" + ex.getMessage());
            addDiagnostic(run.getRunId(), "ERROR", null, ex.getMessage(), null);
            run.setStatus(valid > 0 ? "PARTIAL_SUCCESS" : "FAILED");
        }

        run.setRequestCount(requests);
        run.setReceivedCount(received);
        run.setValidCount(valid);
        run.setCreatedCount(created);
        run.setUpdatedCount(updated);
        run.setFailedCount(failed);
        run.setDiagnostics(trim(String.join(" | ", diagnostics), 2000));
        run.setEndedAt(Instant.now());
        runRepository.save(run);
        return toRunDto(run);
    }

    @Scheduled(cron = "${jobhelper.sync.cron:0 0 6 * * *}")
    @Transactional
    public void scheduledSync() {
        for (JobSourceEntity source : sourceRepository.findAll()) {
            if (!source.isEnabled()) {
                continue;
            }
            if (!"FREEHIRE_API".equals(source.getAdapterType()) && !"JOBSPY".equals(source.getAdapterType())) {
                continue;
            }
            try {
                runSync(source.getSourceId(), "SCHEDULED");
            } catch (ApiException ex) {
                if ("SOURCE_RUN_ALREADY_ACTIVE".equals(ex.getCode())) {
                    log.info("Skip scheduled sync for {}: already active", source.getCode());
                } else {
                    log.warn("Scheduled sync failed for {}: {}", source.getCode(), ex.getMessage());
                }
            } catch (Exception ex) {
                log.warn("Scheduled sync failed for {}: {}", source.getCode(), ex.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listRuns() {
        return Map.of(
                "content", runRepository.findTop20ByOrderByCreatedAtDesc().stream().map(this::toRunDto).toList(),
                "totalElements", runRepository.count());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRun(UUID runId) {
        JobSourceRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "Source run not found"));
        Map<String, Object> dto = new java.util.HashMap<>(toRunDto(run));
        List<Map<String, Object>> diag = diagnosticRepository.findByRunIdOrderByOccurredAtAsc(runId).stream()
                .map(d -> Map.<String, Object>of(
                        "diagnosticId", d.getDiagnosticId(),
                        "category", d.getCategory(),
                        "site", d.getSite() == null ? "" : d.getSite(),
                        "message", d.getMessage() == null ? "" : d.getMessage(),
                        "itemKey", d.getItemKey() == null ? "" : d.getItemKey(),
                        "occurredAt", d.getOccurredAt().toString()))
                .toList();
        dto.put("diagnostics", diag.isEmpty() && run.getDiagnostics() != null
                ? List.of(Map.of("category", "SUMMARY", "message", run.getDiagnostics()))
                : diag);
        return dto;
    }

    private JobSpyResult fetchJobSpy(List<String> terms, List<String> diagnostics, UUID runId, List<String> siteScope) {
        Path scriptDir = resolveJobspyDir();
        if (scriptDir == null) {
            String msg = "JobSpy script dir not found; set jobhelper.jobspy.script or place scripts/datasource";
            diagnostics.add(msg);
            addDiagnostic(runId, "JOBSPY_SETUP", null, msg, null);
            return new JobSpyResult(List.of());
        }
        Path script = scriptDir.resolve("jobspy_fetch.py");
        if (!Files.exists(script)) {
            String msg = "jobspy_fetch.py missing at " + script;
            diagnostics.add(msg);
            addDiagnostic(runId, "JOBSPY_SETUP", null, msg, null);
            return new JobSpyResult(List.of());
        }
        try {
            String python = resolvePython();
            ProcessBuilder pb = new ProcessBuilder(python, script.getFileName().toString());
            pb.directory(scriptDir.toFile());
            pb.redirectErrorStream(true);
            pb.environment().put("JOBHELPER_SEARCH_TERMS", String.join(",", terms));
            pb.environment().put("JOBHELPER_SITES", siteScope != null && !siteScope.isEmpty()
                    ? String.join(",", siteScope)
                    : "linkedin,indeed");
            pb.environment().put("JOBHELPER_RESULTS_WANTED", String.valueOf(Math.max(15, jobspyResultsWanted)));
            Process process = pb.start();
            String output;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = br.lines().collect(Collectors.joining("\n"));
            }
            boolean finished = process.waitFor(jobspyTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                String msg = "JobSpy timed out after " + jobspyTimeoutSeconds + "s";
                diagnostics.add(msg);
                addDiagnostic(runId, "JOBSPY_TIMEOUT", null, msg, null);
                return new JobSpyResult(List.of());
            }
            int code = process.exitValue();
            if (code != 0) {
                diagnostics.add("JOBSPY_EXIT_" + code);
                addDiagnostic(runId, "JOBSPY_EXIT", null, "exit=" + code + " " + trim(output, 500), null);
            }
            Path latest = scriptDir.resolve("out").resolve("jobspy-latest.json");
            if (!Files.exists(latest)) {
                String msg = "jobspy-latest.json not produced";
                diagnostics.add(msg);
                addDiagnostic(runId, "JOBSPY_OUTPUT", null, msg, null);
                return new JobSpyResult(List.of());
            }
            JsonNode root = objectMapper.readTree(Files.readString(latest));
            List<RawJob> jobs = new ArrayList<>();
            JsonNode errors = root.path("errors");
            if (errors.isArray()) {
                for (JsonNode err : errors) {
                    String site = text(err, "label");
                    String message = text(err, "error");
                    addDiagnostic(runId, "JOBSPY_SITE_ERROR", site, message, null);
                    diagnostics.add("SITE_ERROR:" + site + ":" + message);
                }
            }
            JsonNode arr = root.path("jobs");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    jobs.add(new RawJob(
                            text(n, "id") != null ? text(n, "id") : text(n, "url"),
                            text(n, "title"),
                            text(n, "company"),
                            text(n, "location") != null ? text(n, "location") : "Ireland",
                            text(n, "url"),
                            null,
                            text(n, "description_snippet"),
                            text(n, "site")));
                }
            }
            return new JobSpyResult(jobs);
        } catch (Exception ex) {
            diagnostics.add("JOBSPY_ERROR:" + ex.getMessage());
            addDiagnostic(runId, "JOBSPY_ERROR", null, ex.getMessage(), null);
            return new JobSpyResult(List.of());
        }
    }

    private Path resolveJobspyDir() {
        if (jobspyScriptProperty != null && !jobspyScriptProperty.isBlank()) {
            Path p = Path.of(jobspyScriptProperty).toAbsolutePath().normalize();
            if (Files.isDirectory(p)) {
                return p;
            }
            if (Files.isRegularFile(p)) {
                return p.getParent();
            }
        }
        Path cur = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 8 && cur != null; i++) {
            Path candidate = cur.resolve("scripts").resolve("datasource");
            if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("jobspy_fetch.py"))) {
                return candidate;
            }
            cur = cur.getParent();
        }
        return null;
    }

    private void addDiagnostic(UUID runId, String category, String site, String message, String itemKey) {
        JobSourceDiagnosticEntity d = new JobSourceDiagnosticEntity();
        d.setDiagnosticId(UUID.randomUUID());
        d.setRunId(runId);
        d.setCategory(trim(category, 64));
        d.setSite(trim(site, 64));
        d.setMessage(trim(message, 1000));
        d.setItemKey(trim(itemKey, 255));
        d.setOccurredAt(Instant.now());
        diagnosticRepository.save(d);
    }

    private UpsertResult upsert(RawJob raw, ProfileEntity profile, String sourceCode) {
        String stableId = raw.stableId() != null ? raw.stableId() : raw.url();
        Instant now = Instant.now();

        // 1) Hard dedupe by (source, stable id) — same adapter re-seeing the same posting.
        var existing = jobRepository.findByPreferredSourceCodeAndSourceStableId(sourceCode, stableId);
        // 2) Hard dedupe by normalized canonical apply URL — a different source landing on the same job.
        if (existing.isEmpty() && raw.url() != null) {
            existing = duplicateDetectionService.findHardDedupeMatchByUrl(raw.url(), null);
        }

        if (existing.isPresent()) {
            CanonicalJobEntity job = existing.get();
            job.setLastSeenAt(now);
            job.setUpdatedAt(now);
            if (!isProtectedStatus(job.getJobStatus())) {
                job.setTitle(trim(raw.title(), 1024));
                job.setCompany(trim(raw.company() != null ? raw.company() : job.getCompany(), 512));
                job.setLocation(trim(raw.location() != null ? raw.location() : job.getLocation(), 512));
                job.setCanonicalApplyUrl(trim(raw.url(), 1024));
                job.setSeniority(trim(raw.seniority(), 64));
                if (raw.description() != null) {
                    job.setDescription(trim(raw.description(), 9000));
                }
                gateRankService.evaluate(job, profile);
            }
            job.setVersion(job.getVersion() + 1);
            jobRepository.save(job);
            duplicateDetectionService.upsertSourceRef(job.getJobId(), sourceCode, stableId, raw.url(), false);
            return new UpsertResult(false, job);
        }

        CanonicalJobEntity job = new CanonicalJobEntity();
        job.setJobId(UUID.randomUUID());
        job.setTitle(trim(raw.title(), 1024));
        job.setCompany(trim(raw.company() != null ? raw.company() : "Unknown", 512));
        job.setLocation(trim(raw.location() != null ? raw.location() : "Ireland", 512));
        job.setJobStatus("NEW");
        job.setValidityStatus("ACTIVE");
        job.setGateStatus("UNKNOWN");
        job.setRankTier("UNRANKED");
        job.setHiddenByDefault(false);
        job.setLastSeenAt(now);
        job.setFirstSeenAt(now);
        job.setPreferredSourceCode(sourceCode);
        job.setSourceStableId(stableId);
        job.setHasPendingDuplicate(false);
        job.setVersion(1);
        job.setDescription(trim(raw.description(), 9000));
        job.setCanonicalApplyUrl(trim(raw.url(), 1024));
        job.setSeniority(trim(raw.seniority(), 64));
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        gateRankService.evaluate(job, profile);
        jobRepository.save(job);
        duplicateDetectionService.upsertSourceRef(job.getJobId(), sourceCode, stableId, raw.url(), true);
        try {
            duplicateDetectionService.detectFuzzyDuplicates(job);
        } catch (Exception ex) {
            log.warn("Fuzzy duplicate detection skipped for {}: {}", job.getJobId(), ex.getMessage());
        }
        outboxService.append("JOB", "JOB_CREATED", "JOB", job.getJobId(),
                "{\"title\":\"" + escape(job.getTitle()) + "\"}");
        return new UpsertResult(true, job);
    }

    private boolean isProtectedStatus(String status) {
        // Background sync must never reset a user decision (job-model §4 invariant 6).
        return "SHORTLISTED".equals(status) || "APPLIED".equals(status)
                || "IGNORED".equals(status) || "ARCHIVED".equals(status);
    }

    /**
     * Facet-first FreeHire discovery (industrial path): junior/intern eng categories + middle eng
     * + keyword terms, each with pagination. Dedupes by stable id across queries.
     */
    private FreeHireFetchResult fetchFreeHireDiscovery(List<String> terms, List<String> diagnostics, UUID runId)
            throws Exception {
        List<FreeHireQuery> queries = buildFreeHireQueries(terms);
        Map<String, RawJob> byId = new LinkedHashMap<>();
        int requests = 0;
        List<String> labels = new ArrayList<>();
        int pageLimit = Math.min(100, Math.max(10, freehireLimit));
        int maxPages = Math.max(1, freehireMaxPages);

        for (FreeHireQuery query : queries) {
            labels.add(query.label());
            int offset = 0;
            Integer total = null;
            for (int page = 0; page < maxPages; page++) {
                requests++;
                String url = buildFreeHireUrl(query.params(), pageLimit, offset);
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(40))
                        .header("Accept", "application/json")
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    String msg = "FREEHIRE_HTTP_" + response.statusCode() + " query=" + query.label();
                    diagnostics.add(msg);
                    addDiagnostic(runId, "FREEHIRE_HTTP", null, msg, query.label());
                    break;
                }
                JsonNode root = objectMapper.readTree(response.body());
                if (total == null) {
                    JsonNode metaTotal = root.path("meta").path("total");
                    if (metaTotal.isIntegralNumber()) {
                        total = metaTotal.asInt();
                    }
                }
                JsonNode data = root.path("data");
                int batchSize = 0;
                if (data.isArray()) {
                    for (JsonNode n : data) {
                        batchSize++;
                        RawJob raw = parseFreeHireJob(n);
                        if (raw.stableId() == null || raw.stableId().isBlank()) {
                            continue;
                        }
                        byId.putIfAbsent(raw.stableId(), raw);
                    }
                }
                offset += pageLimit;
                if (batchSize < pageLimit) {
                    break;
                }
                if (total != null && offset >= total) {
                    break;
                }
                Thread.sleep(200);
            }
        }
        addDiagnostic(runId, "FREEHIRE_SUMMARY", null,
                "queries=" + labels.size() + " unique=" + byId.size() + " requests=" + requests,
                null);
        return new FreeHireFetchResult(List.copyOf(byId.values()), requests, String.join(",", labels));
    }

    private List<FreeHireQuery> buildFreeHireQueries(List<String> terms) {
        List<FreeHireQuery> queries = new ArrayList<>();
        String engCategories = "backend,frontend,fullstack,qa,ml_ai,ai_engineering,devops,mobile,data_engineering";
        queries.add(new FreeHireQuery("ie-junior-eng", Map.of(
                "countries", "ie",
                "seniority", "junior,intern",
                "category", engCategories)));
        queries.add(new FreeHireQuery("ie-middle-eng", Map.of(
                "countries", "ie",
                "seniority", "middle",
                "category", engCategories)));
        queries.add(new FreeHireQuery("ie-grad-q", Map.of(
                "countries", "ie",
                "q", "graduate software")));
        queries.add(new FreeHireQuery("ie-junior-q", Map.of(
                "countries", "ie",
                "q", "junior software engineer")));
        for (String term : terms) {
            if (term == null || term.isBlank()) {
                continue;
            }
            queries.add(new FreeHireQuery("q-" + slugLabel(term), Map.of(
                    "countries", "ie",
                    "q", term.trim())));
        }
        return queries;
    }

    private String buildFreeHireUrl(Map<String, String> params, int limit, int offset) {
        StringBuilder sb = new StringBuilder("https://freehire.me/api/v1/jobs/search?");
        boolean first = true;
        for (var e : params.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        sb.append("&limit=").append(limit)
                .append("&offset=").append(offset)
                .append("&semantic_ratio=0");
        return sb.toString();
    }

    private RawJob parseFreeHireJob(JsonNode n) {
        String location = text(n, "location");
        if (location == null || location.isBlank()) {
            JsonNode cities = n.path("cities");
            if (cities.isArray() && cities.size() > 0) {
                location = cities.get(0).asText("Ireland");
            } else {
                location = "Ireland";
            }
        }
        String seniority = null;
        JsonNode enrichment = n.path("enrichment");
        if (!enrichment.isMissingNode()) {
            seniority = text(enrichment, "seniority");
        }
        return new RawJob(
                text(n, "public_slug") != null ? text(n, "public_slug") : text(n, "external_id"),
                text(n, "title"),
                text(n, "company"),
                location,
                text(n, "url"),
                seniority,
                text(n, "description"),
                "freehire");
    }

    private List<String> expandJobSpyTerms(List<String> terms) {
        Set<String> out = new LinkedHashSet<>();
        out.add("junior software engineer");
        out.add("graduate software developer");
        out.add("graduate software engineer");
        out.add("junior backend developer");
        if (terms != null) {
            out.addAll(terms);
        }
        return List.copyOf(out);
    }

    private String slugLabel(String term) {
        return term.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }

    private String resolvePython() {
        Path brew = Path.of("/opt/homebrew/bin/python3");
        if (Files.isExecutable(brew)) {
            return brew.toString();
        }
        Path sys = Path.of("/usr/bin/python3");
        if (Files.isExecutable(sys)) {
            return sys.toString();
        }
        return "python3";
    }

    private List<String> resolveSearchTerms() {
        return searchSettingsRepository.findById(SEARCH_SETTINGS_ID)
                .map(JobSearchSettingsEntity::getSearchTerms)
                .map(this::parseSearchTermsString)
                .orElseGet(List::of);
    }

    private List<String> normalizeSearchTerms(Object searchTermsBody) {
        if (searchTermsBody instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
        }
        if (searchTermsBody == null) {
            return List.of();
        }
        return parseSearchTermsString(String.valueOf(searchTermsBody));
    }

    private List<String> parseSearchTermsString(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> terms = new ArrayList<>();
        for (String part : raw.split("[,\\n]")) {
            if (!part.isBlank()) {
                terms.add(part.trim());
            }
        }
        return terms.stream().distinct().toList();
    }

    private void requireSearchTerms() {
        if (resolveSearchTerms().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SEARCH_TERMS_REQUIRED",
                    "Fill global search terms before syncing");
        }
    }

    private Map<String, Object> sourceDto(JobSourceEntity s, List<String> globalTerms) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sourceId", s.getSourceId());
        m.put("code", s.getCode());
        m.put("enabled", s.isEnabled());
        m.put("role", s.getRole());
        m.put("searchTerms", globalTerms);
        m.put("riskNote", s.getRiskNote() == null ? "" : s.getRiskNote());
        return m;
    }

    private Map<String, Object> toRunDto(JobSourceRunEntity run) {
        return Map.ofEntries(
                Map.entry("runId", run.getRunId()),
                Map.entry("sourceId", run.getSourceId()),
                Map.entry("triggerType", run.getTriggerType()),
                Map.entry("status", run.getStatus()),
                Map.entry("parametersSnapshot", run.getParametersSnapshot() == null ? "" : run.getParametersSnapshot()),
                Map.entry("requestCount", run.getRequestCount()),
                Map.entry("receivedCount", run.getReceivedCount()),
                Map.entry("validCount", run.getValidCount()),
                Map.entry("createdCount", run.getCreatedCount()),
                Map.entry("updatedCount", run.getUpdatedCount()),
                Map.entry("failedCount", run.getFailedCount()),
                Map.entry("diagnostics", run.getDiagnostics() == null ? List.of() : List.of(run.getDiagnostics())),
                Map.entry("startedAt", run.getStartedAt() == null ? "" : run.getStartedAt().toString()),
                Map.entry("endedAt", run.getEndedAt() == null ? "" : run.getEndedAt().toString()));
    }

    private String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private String trim(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "'");
    }

    /** Reads {@code searchTerms} off this source's most recent run's snapshot, if any. */
    private Optional<List<String>> lastRunSearchTerms(UUID sourceId) {
        return runRepository.findTopBySourceIdOrderByCreatedAtDesc(sourceId)
                .map(JobSourceRunEntity::getParametersSnapshot)
                .filter(snapshot -> snapshot != null && !snapshot.isBlank())
                .flatMap(snapshot -> {
                    try {
                        JsonNode termsNode = objectMapper.readTree(snapshot).path("searchTerms");
                        if (!termsNode.isArray() || termsNode.isEmpty()) {
                            return Optional.<List<String>>empty();
                        }
                        List<String> terms = new ArrayList<>();
                        termsNode.forEach(n -> terms.add(n.asText()));
                        return Optional.of(terms);
                    } catch (Exception ex) {
                        return Optional.<List<String>>empty();
                    }
                });
    }

    private String buildParametersSnapshot(List<String> terms, List<String> siteScope, boolean reuseLastParameters) {
        StringBuilder sb = new StringBuilder("{\"searchTerms\":").append(toJsonArray(terms))
                .append(",\"location\":\"Ireland\"")
                .append(",\"reuseLastParameters\":").append(reuseLastParameters);
        if (!siteScope.isEmpty()) {
            sb.append(",\"siteScope\":").append(toJsonArray(siteScope));
        }
        return sb.append('}').toString();
    }

    private String toJsonArray(List<String> terms) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(terms.get(i).replace("\"", "'")).append('"');
        }
        return sb.append(']').toString();
    }

    private record RawJob(
            String stableId,
            String title,
            String company,
            String location,
            String url,
            String seniority,
            String description,
            String site) {}

    private record UpsertResult(boolean created, CanonicalJobEntity job) {}

    private record JobSpyResult(List<RawJob> jobs) {}

    private record FreeHireQuery(String label, Map<String, String> params) {}

    private record FreeHireFetchResult(List<RawJob> jobs, int requests, String queryLabels) {}
}
