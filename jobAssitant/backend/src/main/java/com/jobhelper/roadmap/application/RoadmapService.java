package com.jobhelper.roadmap.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import com.jobhelper.profile.infrastructure.EducationPeriodEntity;
import com.jobhelper.profile.infrastructure.ProfileEntity;
import com.jobhelper.profile.infrastructure.ProfileRepository;
import com.jobhelper.profile.infrastructure.WorkAuthorizationEntity;
import com.jobhelper.roadmap.domain.GrsAnchorResolver;
import com.jobhelper.roadmap.domain.TemplateMarkdownParser;
import com.jobhelper.roadmap.infrastructure.GenerationRecordEntity;
import com.jobhelper.roadmap.infrastructure.GenerationRecordRepository;
import com.jobhelper.roadmap.infrastructure.PreviewSessionEntity;
import com.jobhelper.roadmap.infrastructure.PreviewSessionRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapTaskEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapTemplateEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapTemplateRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapTemplateVersionEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapTemplateVersionRepository;
import com.jobhelper.roadmap.infrastructure.TaskDependencyEntity;
import com.jobhelper.roadmap.infrastructure.TaskDependencyOverrideEntity;
import com.jobhelper.roadmap.infrastructure.TaskDependencyOverrideRepository;
import com.jobhelper.roadmap.infrastructure.TaskDependencyRepository;
import com.jobhelper.roadmap.infrastructure.TemplateTaskDefinitionEntity;
import com.jobhelper.roadmap.infrastructure.TemplateTaskDefinitionRepository;
import com.jobhelper.shared.outbox.OutboxService;
import com.jobhelper.shared.web.ApiException;

@Service
public class RoadmapService {
    private static final String SYSTEM_TEMPLATE_CODE = "SYSTEM_IE_GRADUATE";
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);

    private final RoadmapRepository roadmapRepository;
    private final ProfileRepository profileRepository;
    private final GenerationRecordRepository generationRecordRepository;
    private final OutboxService outboxService;
    private final RoadmapTemplateRepository roadmapTemplateRepository;
    private final RoadmapTemplateVersionRepository roadmapTemplateVersionRepository;
    private final TemplateTaskDefinitionRepository templateTaskDefinitionRepository;
    private final PreviewSessionRepository previewSessionRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskDependencyOverrideRepository taskDependencyOverrideRepository;
    private final ObjectMapper objectMapper;

    public RoadmapService(
            RoadmapRepository roadmapRepository,
            ProfileRepository profileRepository,
            GenerationRecordRepository generationRecordRepository,
            OutboxService outboxService,
            RoadmapTemplateRepository roadmapTemplateRepository,
            RoadmapTemplateVersionRepository roadmapTemplateVersionRepository,
            TemplateTaskDefinitionRepository templateTaskDefinitionRepository,
            PreviewSessionRepository previewSessionRepository,
            TaskDependencyRepository taskDependencyRepository,
            TaskDependencyOverrideRepository taskDependencyOverrideRepository,
            ObjectMapper objectMapper) {
        this.roadmapRepository = roadmapRepository;
        this.profileRepository = profileRepository;
        this.generationRecordRepository = generationRecordRepository;
        this.outboxService = outboxService;
        this.roadmapTemplateRepository = roadmapTemplateRepository;
        this.roadmapTemplateVersionRepository = roadmapTemplateVersionRepository;
        this.templateTaskDefinitionRepository = templateTaskDefinitionRepository;
        this.previewSessionRepository = previewSessionRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.taskDependencyOverrideRepository = taskDependencyOverrideRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getActive() {
        RoadmapEntity roadmap = roadmapRepository.findFirstByStatusOrderByUpdatedAtDesc("ACTIVE")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROADMAP_NOT_FOUND", "No ACTIVE roadmap"));
        return toDto(roadmap);
    }

    /** Destructive rebuild — kept for internal/legacy callers; prefer generate/confirm or mergeOrGenerate. */
    @Transactional
    public Map<String, Object> generateOrRefreshFromProfile() {
        ProfileEntity profile = loadProfile();
        RoadmapTemplateVersionEntity systemVersion = resolveSystemTemplateVersion(null);
        List<TemplateTask> templates = loadTemplateTasks(systemVersion.getTemplateVersionId());
        return applyFullGenerate(profile, null, templates, systemVersion.getTemplateVersionId());
    }

    @Transactional
    public Map<String, Object> generatePreview(Integer profileVersion, UUID systemTemplateVersionId, List<UUID> userTemplateVersionIds) {
        ProfileEntity profile = loadProfile();
        if (profileVersion != null && !profileVersion.equals(profile.getProfileVersion())) {
            throw new ApiException(HttpStatus.CONFLICT, "PROFILE_VERSION_CONFLICT",
                    "Profile version mismatch", Map.of("currentProfileVersion", profile.getProfileVersion()));
        }
        String inputHash = inputHash(profile, "GENERATE");
        var existing = generationRecordRepository.findFirstByInputHashAndModeOrderByCreatedAtDesc(inputHash, "GENERATE");
        if (existing.isPresent()) {
            UUID roadmapId = existing.get().getRoadmapId();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("previewToken", existing.get().getPreviewToken());
            body.put("inputHash", inputHash);
            body.put("idempotent", true);
            body.put("existingRoadmapId", roadmapId);
            body.put("candidateTasks", List.of());
            body.put("candidates", List.of());
            body.put("message", "Same inputHash already confirmed");
            return body;
        }

        RoadmapTemplateVersionEntity systemVersion = resolveSystemTemplateVersion(systemTemplateVersionId);
        List<TemplateTask> templates = loadCombinedTemplates(systemVersion.getTemplateVersionId(), userTemplateVersionIds);
        AnchorBundle anchors = resolveAnchors(profile);

        List<Map<String, Object>> candidates = new ArrayList<>();
        List<Map<String, Object>> unscheduled = new ArrayList<>();
        for (TemplateTask t : templates) {
            LocalDate anchor = anchors.forKey(t.anchor());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("logicalTaskKey", t.key());
            row.put("title", t.title());
            row.put("phase", t.phase());
            row.put("anchor", t.anchor());
            row.put("dependsOnKeys", t.dependsOn());
            row.put("completionCriteria", t.criteria());
            row.put("priority", t.priority());
            row.put("origin", "SYSTEM_TEMPLATE");
            if (anchor == null) {
                row.put("dueAt", null);
                row.put("actionability", "UNSCHEDULED");
                unscheduled.add(Map.of("logicalTaskKey", t.key(), "missingAnchor", t.anchor() == null ? "" : t.anchor()));
            } else {
                Instant due = anchor.plusDays(t.offsetDays()).atStartOfDay().toInstant(ZoneOffset.UTC);
                row.put("dueAt", due.toString());
                row.put("actionability", "ACTIONABLE");
            }
            candidates.add(row);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mode", "GENERATE");
        payload.put("inputHash", inputHash);
        payload.put("profileVersion", profile.getProfileVersion());
        payload.put("systemTemplateVersionId", systemVersion.getTemplateVersionId().toString());
        payload.put("userTemplateVersionIds", userTemplateVersionIds == null ? List.of()
                : userTemplateVersionIds.stream().map(UUID::toString).toList());
        PreviewSessionEntity session = createPreviewSession("GENERATE", payload);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("previewToken", session.getPreviewToken());
        body.put("expiresAt", session.getExpiresAt());
        body.put("inputHash", inputHash);
        body.put("profileVersion", profile.getProfileVersion());
        body.put("anchorSnapshot", anchors.snapshot());
        body.put("ruleVersion", "RDM-ANCHOR-GRS-001@v1,RDM-SYSTEM-TEMPLATE@v1");
        body.put("candidateTasks", candidates);
        body.put("candidates", candidates);
        body.put("unscheduledTasks", unscheduled);
        body.put("mergePolicy", "FULL_GENERATE");
        return body;
    }

    @Transactional
    public Map<String, Object> generateConfirm(String previewToken, boolean confirmPreview) {
        if (!confirmPreview) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "confirmPreview must be true");
        }
        PreviewSessionEntity session = requirePreviewSession(previewToken, "GENERATE");
        Map<String, Object> payload = readJson(session.getPayload());
        String inputHash = str(payload.get("inputHash"), null);
        var existing = generationRecordRepository.findFirstByInputHashAndModeOrderByCreatedAtDesc(inputHash, "GENERATE");
        if (existing.isPresent()) {
            RoadmapEntity roadmap = roadmapRepository.findById(existing.get().getRoadmapId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROADMAP_NOT_FOUND", "Roadmap missing"));
            previewSessionRepository.deleteById(previewToken);
            return toDto(roadmap);
        }
        ProfileEntity profile = loadProfile();
        UUID systemVersionId = payload.get("systemTemplateVersionId") != null
                ? UUID.fromString(String.valueOf(payload.get("systemTemplateVersionId"))) : null;
        List<UUID> userVersionIds = extractUuidList(payload.get("userTemplateVersionIds"));
        RoadmapTemplateVersionEntity systemVersion = resolveSystemTemplateVersion(systemVersionId);
        List<TemplateTask> templates = loadCombinedTemplates(systemVersion.getTemplateVersionId(), userVersionIds);
        Map<String, Object> dto = applyFullGenerate(profile, inputHash, templates, systemVersion.getTemplateVersionId());
        recordGeneration(UUID.fromString(String.valueOf(dto.get("roadmapId"))), "GENERATE", inputHash,
                profile.getProfileVersion(), previewToken);
        previewSessionRepository.deleteById(previewToken);
        return dto;
    }

    @Transactional
    public Map<String, Object> buildRecomputePreview() {
        ProfileEntity profile = loadProfile();
        String inputHash = inputHash(profile, "RECOMPUTE");
        AnchorBundle anchors = resolveAnchors(profile);
        RoadmapEntity roadmap = roadmapRepository.findFirstByStatusOrderByUpdatedAtDesc("ACTIVE").orElse(null);
        UUID systemVersionId = (roadmap != null && roadmap.getSystemTemplateVersionId() != null)
                ? roadmap.getSystemTemplateVersionId()
                : resolveSystemTemplateVersion(null).getTemplateVersionId();
        List<TemplateTask> templates = loadTemplateTasks(systemVersionId);
        Map<String, TemplateTask> templateByKey = templates.stream()
                .collect(Collectors.toMap(TemplateTask::key, t -> t, (a, b) -> a, LinkedHashMap::new));

        List<Map<String, Object>> tasksToUpdate = new ArrayList<>();
        List<Map<String, Object>> protectedTasks = new ArrayList<>();
        if (roadmap != null) {
            for (RoadmapTaskEntity task : roadmap.getTasks()) {
                String reason = protectionReason(task);
                if (reason != null) {
                    protectedTasks.add(Map.of("taskId", task.getTaskId(), "reason", reason));
                    continue;
                }
                TemplateTask t = templateByKey.get(task.getLogicalTaskKey());
                if (t == null) {
                    continue;
                }
                LocalDate anchor = anchors.forKey(t.anchor());
                Instant newDue = anchor == null ? null : anchor.plusDays(t.offsetDays()).atStartOfDay().toInstant(ZoneOffset.UTC);
                String newActionability = newDue == null ? "UNSCHEDULED" : "ACTIONABLE";
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("taskId", task.getTaskId());
                row.put("logicalTaskKey", task.getLogicalTaskKey());
                row.put("title", task.getTitle());
                row.put("oldDueAt", task.getDueAt());
                row.put("newDueAt", newDue);
                row.put("oldActionability", task.getActionability());
                row.put("newActionability", newActionability);
                tasksToUpdate.add(row);
            }
        } else {
            for (TemplateTask t : templates) {
                LocalDate anchor = anchors.forKey(t.anchor());
                Instant due = anchor == null ? null : anchor.plusDays(t.offsetDays()).atStartOfDay().toInstant(ZoneOffset.UTC);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("taskId", null);
                row.put("logicalTaskKey", t.key());
                row.put("title", t.title());
                row.put("oldDueAt", null);
                row.put("newDueAt", due);
                row.put("oldActionability", null);
                row.put("newActionability", due == null ? "UNSCHEDULED" : "ACTIONABLE");
                tasksToUpdate.add(row);
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mode", "RECOMPUTE");
        payload.put("inputHash", inputHash);
        payload.put("profileVersion", profile.getProfileVersion());
        payload.put("systemTemplateVersionId", systemVersionId.toString());
        PreviewSessionEntity session = createPreviewSession("RECOMPUTE", payload);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("previewToken", session.getPreviewToken());
        body.put("expiresAt", session.getExpiresAt());
        body.put("inputHash", inputHash);
        body.put("profileVersion", profile.getProfileVersion());
        body.put("anchorSnapshot", anchors.snapshot());
        body.put("ruleVersion", "RDM-ANCHOR-GRS-001@v1,RDM-SYSTEM-TEMPLATE@v1");
        body.put("tasksToUpdate", tasksToUpdate);
        body.put("protectedTasks", protectedTasks);
        body.put("protectedTaskCount", protectedTasks.size());
        body.put("mergePolicy", "SYSTEM_INCOMPLETE_ONLY");
        return body;
    }

    @Transactional
    public Map<String, Object> recomputePreview(Integer profileVersion, UUID recomputeRequestId) {
        Map<String, Object> preview = buildRecomputePreview();
        if (profileVersion != null) {
            preview.put("requestedProfileVersion", profileVersion);
        }
        if (recomputeRequestId != null) {
            preview.put("recomputeRequestId", recomputeRequestId);
        }
        return preview;
    }

    @Transactional
    public Map<String, Object> recomputeConfirm(String previewToken, boolean confirmPreview) {
        if (!confirmPreview) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "confirmPreview must be true");
        }
        return mergeOrGenerate(previewToken);
    }

    @Transactional
    public Map<String, Object> mergeOrGenerate(String previewToken) {
        PreviewSessionEntity session = previewToken == null ? null : previewSessionRepository.findById(previewToken).orElse(null);
        if (session == null) {
            // Accept recompute tokens stored only on recompute_request: generate merge without session
            return mergeSystemIncompleteOnly(loadProfile(), null);
        }
        if (Instant.now().isAfter(session.getExpiresAt())) {
            previewSessionRepository.deleteById(previewToken);
            throw new ApiException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED", "Preview token expired");
        }
        Map<String, Object> payload = readJson(session.getPayload());
        String mode = str(payload.get("mode"), session.getKind());
        ProfileEntity profile = loadProfile();
        String inputHash = str(payload.get("inputHash"), null);
        if ("GENERATE".equals(mode)) {
            UUID systemVersionId = payload.get("systemTemplateVersionId") != null
                    ? UUID.fromString(String.valueOf(payload.get("systemTemplateVersionId"))) : null;
            List<UUID> userVersionIds = extractUuidList(payload.get("userTemplateVersionIds"));
            RoadmapTemplateVersionEntity systemVersion = resolveSystemTemplateVersion(systemVersionId);
            List<TemplateTask> templates = loadCombinedTemplates(systemVersion.getTemplateVersionId(), userVersionIds);
            Map<String, Object> dto = applyFullGenerate(profile, inputHash, templates, systemVersion.getTemplateVersionId());
            recordGeneration(UUID.fromString(String.valueOf(dto.get("roadmapId"))), "GENERATE", inputHash,
                    profile.getProfileVersion(), previewToken);
            previewSessionRepository.deleteById(previewToken);
            return dto;
        }
        Map<String, Object> result = mergeSystemIncompleteOnly(profile, inputHash);
        recordGeneration(UUID.fromString(String.valueOf(result.get("roadmapId"))), "RECOMPUTE", inputHash,
                profile.getProfileVersion(), previewToken);
        previewSessionRepository.deleteById(previewToken);
        return result;
    }

    @Transactional
    public Map<String, Object> createTask(Map<String, Object> body) {
        RoadmapEntity roadmap = requireActive();
        RoadmapTaskEntity task = new RoadmapTaskEntity();
        task.setTaskId(UUID.randomUUID());
        task.setRoadmap(roadmap);
        task.setLogicalTaskKey(str(body.get("logicalTaskKey"), "user." + UUID.randomUUID()));
        task.setTitle(str(body.get("title"), "Untitled task"));
        task.setPhase(str(body.get("phase"), "自定义"));
        task.setStatus("TODO");
        task.setActionability(str(body.get("actionability"), "ACTIONABLE"));
        task.setDueAt(parseInstant(body.get("dueAt")));
        task.setOrigin("USER_CREATED");
        task.setPriority(str(body.get("priority"), "MEDIUM"));
        task.setUserPinned(false);
        task.setUserEdited(true);
        task.setCompletionCriteria(str(body.get("completionCriteria"), null));
        task.setSortOrder(roadmap.getTasks().size());
        roadmap.getTasks().add(task);
        roadmap.setUpdatedAt(Instant.now());
        roadmap.setVersion(roadmap.getVersion() + 1);
        roadmapRepository.save(roadmap);
        maybeEmitActionable(task, roadmap);
        return taskDto(task, blockingIdsFor(roadmap, task));
    }

    @Transactional
    public Map<String, Object> patchTask(UUID taskId, Map<String, Object> body) {
        RoadmapEntity roadmap = requireActive();
        RoadmapTaskEntity task = findTask(roadmap, taskId);
        if (body.containsKey("title")) {
            task.setTitle(str(body.get("title"), task.getTitle()));
        }
        if (body.containsKey("dueAt")) {
            task.setDueAt(parseInstant(body.get("dueAt")));
        }
        if (body.containsKey("priority")) {
            task.setPriority(str(body.get("priority"), task.getPriority()));
        }
        if (body.containsKey("completionCriteria")) {
            task.setCompletionCriteria(str(body.get("completionCriteria"), null));
        }
        if (body.containsKey("phase")) {
            task.setPhase(str(body.get("phase"), task.getPhase()));
        }
        task.setUserEdited(true);
        roadmap.setUpdatedAt(Instant.now());
        roadmap.setVersion(roadmap.getVersion() + 1);
        roadmapRepository.save(roadmap);
        maybeEmitActionable(task, roadmap);
        return taskDto(task, blockingIdsFor(roadmap, task));
    }

    @Transactional
    public Map<String, Object> startTask(UUID taskId) {
        return transition(taskId, "IN_PROGRESS");
    }

    @Transactional
    public Map<String, Object> completeTask(UUID taskId, Map<String, Object> body) {
        RoadmapEntity roadmap = requireActive();
        RoadmapTaskEntity task = findTask(roadmap, taskId);
        List<UUID> blockingIds = blockingIdsFor(roadmap, task);
        Map<String, Object> overrideBody = body == null ? null : asMap(body.get("dependencyOverride"));
        String overrideReason = overrideBody == null ? null : str(overrideBody.get("reason"), null);
        boolean hasOverride = taskDependencyOverrideRepository.existsById(taskId);
        if (!blockingIds.isEmpty()) {
            if (overrideReason == null || overrideReason.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "TASK_DEPENDENCIES_UNMET",
                        "Prerequisite tasks are not complete", Map.of("blockingTaskIds", blockingIds));
            }
            TaskDependencyOverrideEntity override = taskDependencyOverrideRepository.findById(taskId)
                    .orElseGet(TaskDependencyOverrideEntity::new);
            override.setTaskId(taskId);
            override.setReason(overrideReason);
            override.setCreatedAt(Instant.now());
            taskDependencyOverrideRepository.save(override);
            hasOverride = true;
        }
        task.setStatus("COMPLETED");
        task.setActionability("COMPLETED");
        roadmap.setUpdatedAt(Instant.now());
        roadmap.setVersion(roadmap.getVersion() + 1);
        roadmapRepository.save(roadmap);
        Map<String, Object> dto = taskDto(task, List.of());
        dto.put("hasDependencyOverride", hasOverride);
        return dto;
    }

    @Transactional
    public Map<String, Object> restoreTask(UUID taskId) {
        return transition(taskId, "TODO");
    }

    @Transactional
    public Map<String, Object> archiveTask(UUID taskId) {
        return transition(taskId, "ARCHIVED");
    }

    @Transactional
    public Map<String, Object> pinTask(UUID taskId, Map<String, Object> body) {
        RoadmapEntity roadmap = requireActive();
        RoadmapTaskEntity task = findTask(roadmap, taskId);
        boolean pinned = body == null || body.get("pinned") == null || Boolean.TRUE.equals(body.get("pinned"));
        task.setUserPinned(pinned);
        roadmap.setUpdatedAt(Instant.now());
        roadmap.setVersion(roadmap.getVersion() + 1);
        roadmapRepository.save(roadmap);
        return taskDto(task, blockingIdsFor(roadmap, task));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getActionability() {
        RoadmapEntity roadmap = requireActive();
        Map<String, Object> dto = toDto(roadmap);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) dto.get("tasks");

        long actionable = 0;
        long blocked = 0;
        long unscheduled = 0;
        Set<Object> blockingTaskIds = new LinkedHashSet<>();
        List<Map<String, Object>> actionableTasks = new ArrayList<>();
        for (Map<String, Object> t : tasks) {
            String a = String.valueOf(t.get("actionability"));
            if ("ACTIONABLE".equals(a)) {
                actionable++;
                actionableTasks.add(t);
            } else if ("BLOCKED".equals(a)) {
                blocked++;
                Object ids = t.get("blockingTaskIds");
                if (ids instanceof List<?> l) {
                    blockingTaskIds.addAll(l);
                }
            } else if ("UNSCHEDULED".equals(a)) {
                unscheduled++;
            }
        }

        List<Object> nextSuggested = actionableTasks.stream()
                .sorted(Comparator
                        .comparing((Map<String, Object> t) -> !Boolean.TRUE.equals(t.get("userPinned")))
                        .thenComparing(t -> priorityRank(String.valueOf(t.get("priority"))))
                        .thenComparing(t -> t.get("dueAt") == null ? "9999" : String.valueOf(t.get("dueAt"))))
                .limit(3)
                .map(t -> t.get("taskId"))
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("actionableCount", actionable);
        body.put("blockedCount", blocked);
        body.put("unscheduledCount", unscheduled);
        body.put("blockingTaskIds", new ArrayList<>(blockingTaskIds));
        body.put("nextSuggestedTaskIds", nextSuggested);
        body.put("items", tasks);
        body.put("homeActionsHint", "跨域优先级见 GET /api/v1/actions");
        return body;
    }

    // ---- Templates -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTemplates() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RoadmapTemplateEntity template : roadmapTemplateRepository.findAll()) {
            Optional<RoadmapTemplateVersionEntity> versionOpt =
                    roadmapTemplateVersionRepository.findFirstByTemplateIdAndStatusOrderByVersionDesc(template.getTemplateId(), "ACTIVE")
                            .or(() -> roadmapTemplateVersionRepository.findFirstByTemplateIdOrderByVersionDesc(template.getTemplateId()));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("templateId", template.getTemplateId());
            m.put("code", template.getCode());
            m.put("name", template.getName());
            m.put("type", template.getType());
            m.put("templateVersionId", versionOpt.map(RoadmapTemplateVersionEntity::getTemplateVersionId).orElse(null));
            m.put("version", versionOpt.map(RoadmapTemplateVersionEntity::getVersion).orElse(0));
            m.put("updatedAt", template.getUpdatedAt());
            result.add(m);
        }
        result.sort(Comparator.comparing(m -> String.valueOf(m.get("type"))));
        return result;
    }

    private UUID resolveLatestSystemTemplateVersionId() {
        RoadmapTemplateEntity system = roadmapTemplateRepository.findByType("SYSTEM").stream().findFirst()
                .or(() -> roadmapTemplateRepository.findByCode("SYSTEM_IE_GRADUATE"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "SYSTEM template not found"));
        return roadmapTemplateVersionRepository
                .findFirstByTemplateIdAndStatusOrderByVersionDesc(system.getTemplateId(), "ACTIVE")
                .or(() -> roadmapTemplateVersionRepository.findFirstByTemplateIdOrderByVersionDesc(system.getTemplateId()))
                .map(RoadmapTemplateVersionEntity::getTemplateVersionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "SYSTEM template version not found"));
    }

    @Transactional
    public Map<String, Object> previewUserTemplate(String name, String markdownContent) {
        TemplateMarkdownParser.ParseResult result = TemplateMarkdownParser.parse(markdownContent);
        if (!result.parseOk()) {
            List<Map<String, Object>> errs = result.errors().stream()
                    .map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("line", e.line());
                        m.put("code", e.code());
                        m.put("message", e.message());
                        return m;
                    })
                    .toList();
            throw new ApiException(HttpStatus.BAD_REQUEST, "TEMPLATE_PARSE_ERROR", "Markdown parse failed", Map.of("errors", errs));
        }
        List<Map<String, Object>> taskMaps = result.tasks().stream().map(this::parsedTaskToMap).toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("markdownContent", markdownContent);
        payload.put("phases", result.phases());
        payload.put("tasks", taskMaps);
        PreviewSessionEntity session = createPreviewSession("USER_TEMPLATE_PREVIEW", payload);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("previewToken", session.getPreviewToken());
        body.put("expiresAt", session.getExpiresAt());
        body.put("parseOk", true);
        body.put("phases", result.phases());
        body.put("tasks", taskMaps);
        body.put("warnings", result.warnings());
        body.put("errors", List.of());
        return body;
    }

    @Transactional
    public Map<String, Object> saveUserTemplate(String name, String markdownContent, boolean previewAccepted, String previewToken) {
        if (!previewAccepted) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "previewAccepted must be true");
        }
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name is required");
        }
        TemplateMarkdownParser.ParseResult result = TemplateMarkdownParser.parse(markdownContent);
        if (!result.parseOk()) {
            List<Map<String, Object>> errs = result.errors().stream()
                    .map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("line", e.line());
                        m.put("code", e.code());
                        m.put("message", e.message());
                        return m;
                    })
                    .toList();
            throw new ApiException(HttpStatus.BAD_REQUEST, "TEMPLATE_PARSE_ERROR", "Markdown parse failed", Map.of("errors", errs));
        }

        String code = "USER_" + slugCode(name);
        RoadmapTemplateEntity template = roadmapTemplateRepository.findByCode(code).orElse(null);
        Instant now = Instant.now();
        int nextVersion = 1;
        if (template == null) {
            template = new RoadmapTemplateEntity();
            template.setTemplateId(UUID.randomUUID());
            template.setCode(code);
            template.setType("USER");
            template.setCreatedAt(now);
        } else {
            nextVersion = roadmapTemplateVersionRepository.findFirstByTemplateIdOrderByVersionDesc(template.getTemplateId())
                    .map(v -> v.getVersion() + 1).orElse(1);
        }
        template.setName(name);
        template.setUpdatedAt(now);
        roadmapTemplateRepository.save(template);

        RoadmapTemplateVersionEntity version = new RoadmapTemplateVersionEntity();
        version.setTemplateVersionId(UUID.randomUUID());
        version.setTemplateId(template.getTemplateId());
        version.setVersion(nextVersion);
        version.setStatus("ACTIVE");
        version.setMarkdownContent(markdownContent);
        version.setPhasesJson(writeJson(result.phases()));
        version.setCreatedAt(now);
        roadmapTemplateVersionRepository.save(version);

        int order = 0;
        List<TemplateTaskDefinitionEntity> defs = new ArrayList<>();
        for (TemplateMarkdownParser.ParsedTask t : result.tasks()) {
            TemplateTaskDefinitionEntity d = new TemplateTaskDefinitionEntity();
            d.setTemplateTaskDefinitionId(UUID.randomUUID());
            d.setTemplateVersionId(version.getTemplateVersionId());
            d.setTemplateTaskKey(t.templateTaskKey());
            d.setTitle(t.title());
            d.setPhase(t.phase());
            d.setAnchorType(t.anchorType());
            d.setRelativeOffsetDays(t.relativeOffsetDays());
            d.setCompletionCriteria(t.completionCriteria());
            d.setPriority(t.priority());
            d.setDependsOnKeys(writeJson(t.dependsOn()));
            d.setSortOrder(order++);
            defs.add(d);
        }
        templateTaskDefinitionRepository.saveAll(defs);

        if (previewToken != null) {
            previewSessionRepository.deleteById(previewToken);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("templateId", template.getTemplateId());
        body.put("templateVersionId", version.getTemplateVersionId());
        body.put("version", version.getVersion());
        return body;
    }

    @Transactional
    public Map<String, Object> previewTemplateUpdates(UUID toVersionId) {
        UUID resolvedVersionId = toVersionId != null ? toVersionId : resolveLatestSystemTemplateVersionId();
        RoadmapTemplateVersionEntity toVersion = roadmapTemplateVersionRepository.findById(resolvedVersionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Template version not found"));
        RoadmapEntity roadmap = requireActive();
        ProfileEntity profile = loadProfile();
        AnchorBundle anchors = resolveAnchors(profile);
        List<TemplateTask> toTasks = loadTemplateTasks(toVersion.getTemplateVersionId());
        Map<String, RoadmapTaskEntity> currentByKey = byLogicalKey(roadmap);

        List<Map<String, Object>> safeAdds = new ArrayList<>();
        List<Map<String, Object>> protectedList = new ArrayList<>();
        List<Map<String, Object>> conflicts = new ArrayList<>();

        for (TemplateTask t : toTasks) {
            RoadmapTaskEntity existing = currentByKey.get(t.key());
            if (existing == null) {
                LocalDate anchor = anchors.forKey(t.anchor());
                Instant due = anchor == null ? null : anchor.plusDays(t.offsetDays()).atStartOfDay().toInstant(ZoneOffset.UTC);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("templateTaskKey", t.key());
                row.put("title", t.title());
                row.put("phase", t.phase());
                row.put("dueAt", due);
                safeAdds.add(row);
            } else if (!"SYSTEM_TEMPLATE".equals(existing.getOrigin())) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("templateTaskKey", t.key());
                row.put("reason", "KEY_COLLISION_WITH_" + existing.getOrigin());
                conflicts.add(row);
            } else {
                String reason = protectionReason(existing);
                if (reason != null) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("templateTaskKey", t.key());
                    row.put("taskId", existing.getTaskId());
                    row.put("reason", reason);
                    protectedList.add(row);
                }
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("toVersionId", resolvedVersionId.toString());
        PreviewSessionEntity session = createPreviewSession("TEMPLATE_UPDATE", payload);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("previewToken", session.getPreviewToken());
        body.put("expiresAt", session.getExpiresAt());
        body.put("safeAdds", safeAdds);
        body.put("protected", protectedList);
        body.put("conflicts", conflicts);
        return body;
    }

    @Transactional
    public Map<String, Object> applyTemplateUpdates(String previewToken, boolean confirmApply) {
        if (!confirmApply) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "confirmApply must be true");
        }
        PreviewSessionEntity session = requirePreviewSession(previewToken, "TEMPLATE_UPDATE");
        Map<String, Object> payload = readJson(session.getPayload());
        UUID toVersionId = UUID.fromString(String.valueOf(payload.get("toVersionId")));

        RoadmapEntity roadmap = requireActive();
        ProfileEntity profile = loadProfile();
        AnchorBundle anchors = resolveAnchors(profile);
        List<TemplateTask> toTasks = loadTemplateTasks(toVersionId);
        Map<String, RoadmapTaskEntity> currentByKey = byLogicalKey(roadmap);

        List<UUID> addedIds = new ArrayList<>();
        int order = roadmap.getTasks().size();
        for (TemplateTask t : toTasks) {
            if (currentByKey.containsKey(t.key())) {
                continue;
            }
            LocalDate anchor = anchors.forKey(t.anchor());
            Instant due = anchor == null ? null : anchor.plusDays(t.offsetDays()).atStartOfDay().toInstant(ZoneOffset.UTC);
            RoadmapTaskEntity task = new RoadmapTaskEntity();
            task.setTaskId(UUID.randomUUID());
            task.setRoadmap(roadmap);
            task.setLogicalTaskKey(t.key());
            task.setTitle(t.title());
            task.setPhase(t.phase());
            task.setStatus("TODO");
            task.setActionability(due == null ? "UNSCHEDULED" : "ACTIONABLE");
            task.setDueAt(due);
            task.setOrigin("SYSTEM_TEMPLATE");
            task.setPriority(t.priority());
            task.setUserPinned(false);
            task.setUserEdited(false);
            task.setCompletionCriteria(t.criteria());
            task.setSortOrder(order++);
            roadmap.getTasks().add(task);
            currentByKey.put(t.key(), task);
            addedIds.add(task.getTaskId());
        }

        roadmap.setSystemTemplateVersionId(toVersionId);
        roadmap.setUpdatedAt(Instant.now());
        roadmap.setVersion(roadmap.getVersion() + 1);
        roadmapRepository.save(roadmap);

        syncDependencyEdges(toTasks, currentByKey);

        for (UUID id : addedIds) {
            currentByKey.values().stream()
                    .filter(x -> x.getTaskId().equals(id))
                    .findFirst()
                    .ifPresent(task -> maybeEmitActionable(task, roadmap));
        }

        previewSessionRepository.deleteById(previewToken);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updatedTaskIds", addedIds);
        body.put("addedCount", addedIds.size());
        body.put("roadmapId", roadmap.getRoadmapId());
        return body;
    }

    // ---- Internal generation / merge --------------------------------------

    private Map<String, Object> applyFullGenerate(ProfileEntity profile, String inputHash, List<TemplateTask> templates, UUID systemTemplateVersionId) {
        AnchorBundle anchors = resolveAnchors(profile);
        RoadmapEntity roadmap = roadmapRepository.findFirstByStatusOrderByUpdatedAtDesc("ACTIVE").orElse(null);
        Instant now = Instant.now();
        if (roadmap == null) {
            roadmap = new RoadmapEntity();
            roadmap.setRoadmapId(UUID.randomUUID());
            roadmap.setStatus("ACTIVE");
            roadmap.setCreatedAt(now);
            roadmap.setVersion(1);
        } else {
            roadmap.getTasks().clear();
            roadmap.setVersion(roadmap.getVersion() + 1);
        }
        roadmap.setProfileVersion(profile.getProfileVersion());
        roadmap.setRuleVersion("RDM-ANCHOR-GRS-001@v1,RDM-SYSTEM-TEMPLATE@v1");
        roadmap.setSystemTemplateVersionId(systemTemplateVersionId);
        roadmap.setUpdatedAt(now);

        Map<String, RoadmapTaskEntity> byKey = new LinkedHashMap<>();
        int order = 0;
        for (TemplateTask t : templates) {
            LocalDate anchor = anchors.forKey(t.anchor());
            Instant due = null;
            String actionability = "ACTIONABLE";
            if (anchor != null) {
                due = anchor.plusDays(t.offsetDays()).atStartOfDay().toInstant(ZoneOffset.UTC);
            } else {
                actionability = "UNSCHEDULED";
            }
            RoadmapTaskEntity task = new RoadmapTaskEntity();
            task.setTaskId(UUID.randomUUID());
            task.setRoadmap(roadmap);
            task.setLogicalTaskKey(t.key());
            task.setTitle(t.title());
            task.setPhase(t.phase());
            task.setStatus("TODO");
            task.setActionability(actionability);
            task.setDueAt(due);
            task.setOrigin("SYSTEM_TEMPLATE");
            task.setPriority(t.priority());
            task.setUserPinned(false);
            task.setUserEdited(false);
            task.setCompletionCriteria(t.criteria());
            task.setSortOrder(order++);
            roadmap.getTasks().add(task);
            byKey.put(t.key(), task);
        }
        roadmapRepository.save(roadmap);
        syncDependencyEdges(templates, byKey);
        for (RoadmapTaskEntity task : roadmap.getTasks()) {
            maybeEmitActionable(task, roadmap);
        }
        return toDto(roadmap);
    }

    private Map<String, Object> mergeSystemIncompleteOnly(ProfileEntity profile, String inputHash) {
        RoadmapEntity roadmap = roadmapRepository.findFirstByStatusOrderByUpdatedAtDesc("ACTIVE").orElse(null);
        if (roadmap == null) {
            RoadmapTemplateVersionEntity systemVersion = resolveSystemTemplateVersion(null);
            List<TemplateTask> templates = loadTemplateTasks(systemVersion.getTemplateVersionId());
            Map<String, Object> created = applyFullGenerate(profile, inputHash, templates, systemVersion.getTemplateVersionId());
            Map<String, Object> summary = new LinkedHashMap<>(created);
            summary.put("resultSummary", Map.of(
                    "updatedTaskIds", ((List<?>) created.get("tasks")).stream()
                            .map(t -> ((Map<?, ?>) t).get("taskId")).toList(),
                    "unchangedProtected", 0,
                    "mode", "FULL_GENERATE"));
            return summary;
        }

        UUID systemVersionId = roadmap.getSystemTemplateVersionId() != null
                ? roadmap.getSystemTemplateVersionId()
                : resolveSystemTemplateVersion(null).getTemplateVersionId();
        List<TemplateTask> templates = loadTemplateTasks(systemVersionId);
        Map<String, TemplateTask> templateByKey = templates.stream()
                .collect(Collectors.toMap(TemplateTask::key, t -> t, (a, b) -> a, LinkedHashMap::new));
        AnchorBundle anchors = resolveAnchors(profile);
        List<UUID> updated = new ArrayList<>();
        int protectedCount = 0;
        Instant now = Instant.now();

        for (RoadmapTaskEntity task : roadmap.getTasks()) {
            if (protectionReason(task) != null) {
                protectedCount++;
                continue;
            }
            TemplateTask t = templateByKey.get(task.getLogicalTaskKey());
            if (t == null) {
                continue;
            }
            LocalDate anchor = anchors.forKey(t.anchor());
            Instant due = null;
            String actionability = "ACTIONABLE";
            if (anchor != null) {
                due = anchor.plusDays(t.offsetDays()).atStartOfDay().toInstant(ZoneOffset.UTC);
            } else {
                actionability = "UNSCHEDULED";
            }
            task.setTitle(t.title());
            task.setPhase(t.phase());
            task.setDueAt(due);
            task.setActionability(actionability);
            task.setPriority(t.priority());
            task.setCompletionCriteria(t.criteria());
            updated.add(task.getTaskId());
        }

        // Add missing system template keys as new TODO tasks
        Set<String> existingKeys = new LinkedHashSet<>();
        for (RoadmapTaskEntity task : roadmap.getTasks()) {
            existingKeys.add(task.getLogicalTaskKey());
        }
        int order = roadmap.getTasks().size();
        for (TemplateTask t : templates) {
            if (existingKeys.contains(t.key())) {
                continue;
            }
            LocalDate anchor = anchors.forKey(t.anchor());
            Instant due = anchor == null ? null : anchor.plusDays(t.offsetDays()).atStartOfDay().toInstant(ZoneOffset.UTC);
            RoadmapTaskEntity task = new RoadmapTaskEntity();
            task.setTaskId(UUID.randomUUID());
            task.setRoadmap(roadmap);
            task.setLogicalTaskKey(t.key());
            task.setTitle(t.title());
            task.setPhase(t.phase());
            task.setStatus("TODO");
            task.setActionability(due == null ? "UNSCHEDULED" : "ACTIONABLE");
            task.setDueAt(due);
            task.setOrigin("SYSTEM_TEMPLATE");
            task.setPriority(t.priority());
            task.setUserPinned(false);
            task.setUserEdited(false);
            task.setCompletionCriteria(t.criteria());
            task.setSortOrder(order++);
            roadmap.getTasks().add(task);
            updated.add(task.getTaskId());
        }

        roadmap.setProfileVersion(profile.getProfileVersion());
        roadmap.setSystemTemplateVersionId(systemVersionId);
        roadmap.setUpdatedAt(now);
        roadmap.setVersion(roadmap.getVersion() + 1);
        roadmapRepository.save(roadmap);

        syncDependencyEdges(templates, byLogicalKey(roadmap));

        for (RoadmapTaskEntity task : roadmap.getTasks()) {
            if (updated.contains(task.getTaskId())) {
                maybeEmitActionable(task, roadmap);
            }
        }

        Map<String, Object> dto = toDto(roadmap);
        dto.put("resultSummary", Map.of(
                "updatedTaskIds", updated,
                "unchangedProtected", protectedCount,
                "mode", "SYSTEM_INCOMPLETE_ONLY",
                "inputHash", inputHash == null ? "" : inputHash));
        return dto;
    }

    private void syncDependencyEdges(List<TemplateTask> templates, Map<String, RoadmapTaskEntity> byKey) {
        List<TaskDependencyEntity> toAdd = new ArrayList<>();
        for (TemplateTask t : templates) {
            RoadmapTaskEntity successor = byKey.get(t.key());
            if (successor == null || t.dependsOn() == null || t.dependsOn().isEmpty()) {
                continue;
            }
            for (String depKey : t.dependsOn()) {
                RoadmapTaskEntity predecessor = byKey.get(depKey);
                if (predecessor == null || predecessor.getTaskId().equals(successor.getTaskId())) {
                    continue;
                }
                if (!taskDependencyRepository.existsByPredecessorTaskIdAndSuccessorTaskId(predecessor.getTaskId(), successor.getTaskId())) {
                    toAdd.add(new TaskDependencyEntity(predecessor.getTaskId(), successor.getTaskId()));
                }
            }
        }
        if (!toAdd.isEmpty()) {
            taskDependencyRepository.saveAll(toAdd);
        }
    }

    private void maybeEmitActionable(RoadmapTaskEntity task, RoadmapEntity roadmap) {
        if (!"ACTIONABLE".equals(task.getActionability())) {
            return;
        }
        if (!"TODO".equals(task.getStatus()) && !"IN_PROGRESS".equals(task.getStatus())) {
            return;
        }
        if (!blockingIdsFor(roadmap, task).isEmpty()) {
            return;
        }
        String payload = "{\"taskId\":\"" + task.getTaskId() + "\",\"title\":\""
                + escape(task.getTitle()) + "\",\"dueAt\":"
                + (task.getDueAt() == null ? "null" : "\"" + task.getDueAt() + "\"")
                + ",\"roadmapId\":\"" + roadmap.getRoadmapId() + "\"}";
        outboxService.append("ROADMAP", "ROADMAP_TASK_ACTIONABLE", "ROADMAP_TASK", task.getTaskId(), payload);
    }

    private void recordGeneration(UUID roadmapId, String mode, String inputHash, int profileVersion, String previewToken) {
        GenerationRecordEntity rec = new GenerationRecordEntity();
        rec.setGenerationRecordId(UUID.randomUUID());
        rec.setRoadmapId(roadmapId);
        rec.setMode(mode);
        rec.setInputHash(inputHash);
        rec.setProfileVersion(profileVersion);
        rec.setPreviewToken(previewToken);
        rec.setCreatedAt(Instant.now());
        generationRecordRepository.save(rec);
    }

    // ---- Preview sessions (durable) ---------------------------------------

    private PreviewSessionEntity createPreviewSession(String kind, Map<String, Object> payload) {
        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        PreviewSessionEntity e = new PreviewSessionEntity();
        e.setPreviewToken(token);
        e.setKind(kind);
        e.setPayload(writeJson(payload));
        e.setCreatedAt(now);
        e.setExpiresAt(now.plus(PREVIEW_TTL));
        return previewSessionRepository.save(e);
    }

    private PreviewSessionEntity requirePreviewSession(String token, String kind) {
        if (token == null) {
            throw new ApiException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED", "Preview token missing");
        }
        PreviewSessionEntity session = previewSessionRepository.findById(token)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED", "Preview token not found or expired"));
        if (Instant.now().isAfter(session.getExpiresAt())) {
            previewSessionRepository.deleteById(token);
            throw new ApiException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED", "Preview token expired");
        }
        if (kind != null && !kind.equals(session.getKind())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Preview token kind mismatch");
        }
        return session;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            Object v = objectMapper.readValue(json, Map.class);
            return v instanceof Map ? (Map<String, Object>) v : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "null";
        }
    }

    private List<UUID> extractUuidList(Object v) {
        if (!(v instanceof List<?> list)) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>();
        for (Object o : list) {
            try {
                out.add(UUID.fromString(String.valueOf(o)));
            } catch (Exception ignored) {
                // skip malformed entries
            }
        }
        return out;
    }

    // ---- Templates: loading ------------------------------------------------

    private RoadmapTemplateVersionEntity resolveSystemTemplateVersion(UUID id) {
        if (id != null) {
            return roadmapTemplateVersionRepository.findById(id)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "GENERATION_INPUT_INVALID", "systemTemplateVersionId not found"));
        }
        RoadmapTemplateEntity template = roadmapTemplateRepository.findByCode(SYSTEM_TEMPLATE_CODE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "System template missing"));
        return roadmapTemplateVersionRepository.findFirstByTemplateIdAndStatusOrderByVersionDesc(template.getTemplateId(), "ACTIVE")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "No ACTIVE system template version"));
    }

    private List<TemplateTask> loadCombinedTemplates(UUID systemVersionId, List<UUID> userVersionIds) {
        List<TemplateTask> templates = new ArrayList<>(loadTemplateTasks(systemVersionId));
        if (userVersionIds != null) {
            Set<String> keys = templates.stream().map(TemplateTask::key).collect(Collectors.toCollection(LinkedHashSet::new));
            for (UUID uid : userVersionIds) {
                for (TemplateTask t : loadTemplateTasks(uid)) {
                    if (keys.add(t.key())) {
                        templates.add(t);
                    }
                }
            }
        }
        return templates;
    }

    private List<TemplateTask> loadTemplateTasks(UUID templateVersionId) {
        List<TemplateTaskDefinitionEntity> defs = templateTaskDefinitionRepository.findByTemplateVersionIdOrderBySortOrderAsc(templateVersionId);
        List<TemplateTask> list = new ArrayList<>();
        for (TemplateTaskDefinitionEntity d : defs) {
            list.add(new TemplateTask(
                    d.getTemplateTaskKey(),
                    d.getTitle(),
                    d.getPhase(),
                    d.getAnchorType(),
                    d.getRelativeOffsetDays(),
                    d.getPriority(),
                    d.getCompletionCriteria(),
                    readStringList(d.getDependsOnKeys())));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Object v = objectMapper.readValue(json, List.class);
            if (v instanceof List<?> l) {
                return l.stream().map(String::valueOf).toList();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return List.of();
    }

    private Map<String, Object> parsedTaskToMap(TemplateMarkdownParser.ParsedTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("templateTaskKey", t.templateTaskKey());
        m.put("title", t.title());
        m.put("phase", t.phase());
        m.put("anchorType", t.anchorType());
        m.put("relativeOffset", t.relativeOffsetDays());
        m.put("dependsOn", t.dependsOn());
        m.put("completionCriteria", t.completionCriteria());
        m.put("priority", t.priority());
        return m;
    }

    private String slugCode(String name) {
        String s = name.toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9\\u4e00-\\u9fa5]+", "_")
                .replaceAll("(^_+|_+$)", "");
        return s.isEmpty() ? "TEMPLATE" : s;
    }

    // ---- Dependency helpers -------------------------------------------------

    private String protectionReason(RoadmapTaskEntity task) {
        if ("COMPLETED".equals(task.getStatus()) || "DONE".equals(task.getStatus())) {
            return "COMPLETED";
        }
        if ("USER_CREATED".equals(task.getOrigin())) {
            return "USER_CREATED";
        }
        if ("USER_TEMPLATE".equals(task.getOrigin())) {
            return "USER_TEMPLATE";
        }
        if (task.isUserEdited()) {
            return "USER_EDITED";
        }
        if (task.isUserPinned()) {
            return "PINNED";
        }
        if (taskDependencyOverrideRepository.existsById(task.getTaskId())) {
            return "DEPENDENCY_OVERRIDE";
        }
        return null;
    }

    private Map<String, RoadmapTaskEntity> byLogicalKey(RoadmapEntity roadmap) {
        Map<String, RoadmapTaskEntity> map = new LinkedHashMap<>();
        for (RoadmapTaskEntity t : roadmap.getTasks()) {
            map.put(t.getLogicalTaskKey(), t);
        }
        return map;
    }

    private Map<UUID, List<UUID>> computeBlockingMap(RoadmapEntity roadmap) {
        List<UUID> taskIds = roadmap.getTasks().stream().map(RoadmapTaskEntity::getTaskId).toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> statusById = roadmap.getTasks().stream()
                .collect(Collectors.toMap(RoadmapTaskEntity::getTaskId, RoadmapTaskEntity::getStatus));
        List<TaskDependencyEntity> deps = taskDependencyRepository.findBySuccessorTaskIdIn(taskIds);
        Map<UUID, List<UUID>> blocking = new LinkedHashMap<>();
        for (TaskDependencyEntity d : deps) {
            String predStatus = statusById.get(d.getPredecessorTaskId());
            if (predStatus != null && !isCompletedStatus(predStatus)) {
                blocking.computeIfAbsent(d.getSuccessorTaskId(), k -> new ArrayList<>()).add(d.getPredecessorTaskId());
            }
        }
        return blocking;
    }

    private List<UUID> blockingIdsFor(RoadmapEntity roadmap, RoadmapTaskEntity task) {
        return computeBlockingMap(roadmap).getOrDefault(task.getTaskId(), List.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object v) {
        return v instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    private int priorityRank(String p) {
        return switch (p) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            case "LOW" -> 2;
            default -> 3;
        };
    }

    private Map<String, Object> transition(UUID taskId, String status) {
        RoadmapEntity roadmap = requireActive();
        RoadmapTaskEntity task = findTask(roadmap, taskId);
        String previousActionability = task.getActionability();
        task.setStatus(status);
        if ("TODO".equals(status) || "IN_PROGRESS".equals(status)) {
            if (task.getDueAt() != null || "ACTIONABLE".equals(previousActionability)) {
                task.setActionability("ACTIONABLE");
            } else {
                task.setActionability("UNSCHEDULED");
            }
        } else if ("ARCHIVED".equals(status)) {
            task.setActionability("ARCHIVED");
        }
        roadmap.setUpdatedAt(Instant.now());
        roadmap.setVersion(roadmap.getVersion() + 1);
        roadmapRepository.save(roadmap);
        maybeEmitActionable(task, roadmap);
        return taskDto(task, blockingIdsFor(roadmap, task));
    }

    private ProfileEntity loadProfile() {
        return profileRepository.findSingleton()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile required"));
    }

    private RoadmapEntity requireActive() {
        return roadmapRepository.findFirstByStatusOrderByUpdatedAtDesc("ACTIVE")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROADMAP_NOT_FOUND", "No ACTIVE roadmap"));
    }

    private RoadmapTaskEntity findTask(RoadmapEntity roadmap, UUID taskId) {
        return roadmap.getTasks().stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found"));
    }

    private AnchorBundle resolveAnchors(ProfileEntity profile) {
        LocalDate graduation = profile.getEducationPeriods().stream()
                .filter(EducationPeriodEntity::isPrimary)
                .map(EducationPeriodEntity::getExpectedGraduationDate)
                .findFirst()
                .orElseGet(() -> profile.getEducationPeriods().stream()
                        .map(EducationPeriodEntity::getExpectedGraduationDate)
                        .filter(d -> d != null)
                        .findFirst()
                        .orElse(null));
        LocalDate courseStart = profile.getEducationPeriods().stream()
                .map(EducationPeriodEntity::getStartDate)
                .filter(d -> d != null)
                .findFirst()
                .orElse(null);
        LocalDate stampExpiry = profile.getWorkAuthorizations().stream()
                .map(WorkAuthorizationEntity::getValidUntil)
                .filter(d -> d != null)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate grs = GrsAnchorResolver.resolve(graduation);
        return new AnchorBundle(courseStart, graduation, grs, stampExpiry);
    }

    private String inputHash(ProfileEntity profile, String mode) {
        AnchorBundle a = resolveAnchors(profile);
        String raw = mode + "|" + profile.getProfileVersion() + "|" + a.snapshot();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private Map<String, Object> toDto(RoadmapEntity roadmap) {
        List<UUID> taskIds = roadmap.getTasks().stream().map(RoadmapTaskEntity::getTaskId).toList();
        List<TaskDependencyEntity> deps = taskIds.isEmpty() ? List.of() : taskDependencyRepository.findBySuccessorTaskIdIn(taskIds);
        Map<UUID, String> statusById = roadmap.getTasks().stream()
                .collect(Collectors.toMap(RoadmapTaskEntity::getTaskId, RoadmapTaskEntity::getStatus));
        Map<UUID, List<UUID>> blockingMap = new LinkedHashMap<>();
        for (TaskDependencyEntity d : deps) {
            String predStatus = statusById.get(d.getPredecessorTaskId());
            if (predStatus != null && !isCompletedStatus(predStatus)) {
                blockingMap.computeIfAbsent(d.getSuccessorTaskId(), k -> new ArrayList<>()).add(d.getPredecessorTaskId());
            }
        }

        Set<String> phases = new LinkedHashSet<>();
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (RoadmapTaskEntity t : roadmap.getTasks()) {
            phases.add(t.getPhase());
            tasks.add(taskDto(t, blockingMap.getOrDefault(t.getTaskId(), List.of())));
        }

        List<Map<String, Object>> dependencies = new ArrayList<>();
        for (TaskDependencyEntity d : deps) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("predecessorTaskId", d.getPredecessorTaskId());
            row.put("successorTaskId", d.getSuccessorTaskId());
            dependencies.add(row);
        }

        Map<String, Object> dto = new HashMap<>();
        dto.put("roadmapId", roadmap.getRoadmapId());
        dto.put("status", roadmap.getStatus());
        dto.put("version", roadmap.getVersion());
        dto.put("profileVersion", roadmap.getProfileVersion());
        dto.put("ruleVersion", roadmap.getRuleVersion());
        dto.put("phases", new ArrayList<>(phases));
        dto.put("tasks", tasks);
        dto.put("modules", tasks);
        dto.put("dependencies", dependencies);
        return dto;
    }

    private Map<String, Object> taskDto(RoadmapTaskEntity t, List<UUID> blockingIds) {
        Map<String, Object> m = new HashMap<>();
        m.put("taskId", t.getTaskId());
        m.put("logicalTaskKey", t.getLogicalTaskKey());
        m.put("title", t.getTitle());
        m.put("phase", t.getPhase());
        m.put("status", t.getStatus());
        m.put("actionability", computeActionability(t, blockingIds));
        m.put("blockingTaskIds", blockingIds);
        m.put("dueAt", t.getDueAt());
        m.put("origin", t.getOrigin());
        m.put("priority", t.getPriority());
        m.put("userPinned", t.isUserPinned());
        m.put("userEdited", t.isUserEdited());
        m.put("completionCriteria", t.getCompletionCriteria());
        return m;
    }

    private String computeActionability(RoadmapTaskEntity t, List<UUID> blockingIds) {
        if (isCompletedStatus(t.getStatus())) {
            return "COMPLETED";
        }
        if ("ARCHIVED".equals(t.getStatus())) {
            return "ARCHIVED";
        }
        if (blockingIds != null && !blockingIds.isEmpty()) {
            return "BLOCKED";
        }
        return t.getActionability();
    }

    private boolean isCompletedStatus(String status) {
        return "COMPLETED".equals(status) || "DONE".equals(status);
    }

    private String str(Object v, String fallback) {
        if (v == null) {
            return fallback;
        }
        String s = String.valueOf(v);
        return s.isBlank() ? fallback : s;
    }

    private Instant parseInstant(Object v) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v);
        if (s.isBlank()) {
            return null;
        }
        return Instant.parse(s);
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "'");
    }

    private record TemplateTask(
            String key, String title, String phase, String anchor, int offsetDays, String priority, String criteria,
            List<String> dependsOn) {}

    private record AnchorBundle(LocalDate courseStart, LocalDate graduation, LocalDate grs, LocalDate stampExpiry) {
        LocalDate forKey(String key) {
            if (key == null) {
                return null;
            }
            return switch (key) {
                case "COURSE_START" -> courseStart;
                case "GRADUATION" -> graduation;
                case "GRADUATE_RECRUITMENT_SEASON" -> grs;
                case "STAMP_EXPIRY" -> stampExpiry;
                default -> null;
            };
        }

        Map<String, String> snapshot() {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("COURSE_START", courseStart == null ? "" : courseStart.toString());
            m.put("GRADUATION", graduation == null ? "" : graduation.toString());
            m.put("GRADUATE_RECRUITMENT_SEASON", grs == null ? "" : grs.toString());
            m.put("STAMP_EXPIRY", stampExpiry == null ? "" : stampExpiry.toString());
            return m;
        }
    }
}
