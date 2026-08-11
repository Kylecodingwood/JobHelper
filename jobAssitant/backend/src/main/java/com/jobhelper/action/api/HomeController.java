package com.jobhelper.action.api;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jobhelper.action.application.ActionPriorityService;
import com.jobhelper.action.application.HomeTodayPriorityService;
import com.jobhelper.action.infrastructure.ActionItemEntity;
import com.jobhelper.action.infrastructure.ActionItemRepository;
import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.job.infrastructure.CanonicalJobRepository;
import com.jobhelper.profile.application.ProfileService;
import com.jobhelper.shared.web.ApiException;

@RestController
@RequestMapping("/api/v1")
public class HomeController {
    private final ActionItemRepository actionItemRepository;
    private final CanonicalJobRepository canonicalJobRepository;
    private final ProfileService profileService;
    private final ActionPriorityService priorityService;
    private final HomeTodayPriorityService todayPriorityService;

    public HomeController(
            ActionItemRepository actionItemRepository,
            CanonicalJobRepository canonicalJobRepository,
            ProfileService profileService,
            ActionPriorityService priorityService,
            HomeTodayPriorityService todayPriorityService) {
        this.actionItemRepository = actionItemRepository;
        this.canonicalJobRepository = canonicalJobRepository;
        this.profileService = profileService;
        this.priorityService = priorityService;
        this.todayPriorityService = todayPriorityService;
    }

    @GetMapping("/home")
    public Map<String, Object> home(
            @RequestParam(defaultValue = "3") int actionLimit,
            @RequestParam(defaultValue = "10") int newJobLimit) {
        if (!profileService.exists()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Complete profile before Home");
        }
        todayPriorityService.refresh();
        int limit = Math.max(1, actionLimit);
        // fetch one extra to know hasMore
        List<ActionItemEntity> fetched = actionItemRepository
                .findByActiveTrueAndStatusOrderByPrioritySortKeyAsc("OPEN", PageRequest.of(0, limit + 1));
        boolean hasMore = fetched.size() > limit;
        List<ActionItemEntity> actions = hasMore ? fetched.subList(0, limit) : fetched;
        List<CanonicalJobEntity> newJobs = canonicalJobRepository
                .findByJobStatusAndHiddenByDefaultFalseOrderByLastSeenAtDesc("NEW", PageRequest.of(0, newJobLimit));

        Map<String, Object> actionsBlock = new HashMap<>();
        actionsBlock.put("content", actions.stream().map(this::toActionSummary).toList());
        actionsBlock.put("priorityRuleVersion", "today-priority-v1");
        actionsBlock.put("hasMore", hasMore);

        Map<String, Object> newJobsBlock = new HashMap<>();
        newJobsBlock.put("content", newJobs.stream().map(this::toJobSummary).toList());
        newJobsBlock.put("source", "/api/v1/jobs/new");

        return Map.of(
                "actions", actionsBlock,
                "newJobs", newJobsBlock,
                "generatedAt", Instant.now().toString());
    }

    @GetMapping("/actions")
    public Map<String, Object> listActions(
            @RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "true") boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ActionItemEntity> slice;
        if (active) {
            slice = actionItemRepository.findByActiveTrueAndStatusOrderByPrioritySortKeyAsc(
                    status, PageRequest.of(page, size));
        } else {
            slice = actionItemRepository.findByStatusOrderByPrioritySortKeyAsc(status, PageRequest.of(page, size));
        }
        var content = slice.stream().map(this::toActionSummary).toList();
        return Map.of("content", content, "totalElements", content.size());
    }

    @GetMapping("/actions/{actionId}")
    public Map<String, Object> detail(@PathVariable UUID actionId) {
        return toActionDetail(load(actionId));
    }

    @GetMapping("/actions/{actionId}/priority-evidence")
    public Map<String, Object> priorityEvidence(@PathVariable UUID actionId) {
        return priorityService.explain(load(actionId));
    }

    @GetMapping("/actions/{actionId}/navigation")
    public Map<String, String> navigation(@PathVariable UUID actionId) {
        ActionItemEntity action = load(actionId);
        String route = action.getRouteHint() != null ? action.getRouteHint() : "/";
        return Map.of("route", route, "focusKey", action.getFocusKey() != null ? action.getFocusKey() : "");
    }

    public record DecisionRequest(String operation, String reason, Integer expectedVersion) {}

    @PostMapping("/actions/{actionId}/decisions")
    public Map<String, Object> decide(@PathVariable UUID actionId, @RequestBody DecisionRequest request) {
        ActionItemEntity action = load(actionId);
        if (!action.isActive() && !"RESTORE".equals(request.operation())) {
            throw new ApiException(HttpStatus.GONE, "ACTION_STALE", "Action is no longer active");
        }
        if (request.expectedVersion() == null || !request.expectedVersion().equals(action.getVersion())) {
            throw new ApiException(HttpStatus.CONFLICT, "ACTION_VERSION_CONFLICT", "Action version mismatch");
        }
        switch (request.operation() == null ? "" : request.operation()) {
            case "PIN" -> {
                action.setPinned(true);
                priorityService.applyEffectiveBand(action);
            }
            case "UNPIN" -> {
                action.setPinned(false);
                priorityService.applyEffectiveBand(action);
            }
            case "COMPLETE" -> {
                action.setStatus("COMPLETED");
                action.setActive(false);
            }
            case "IGNORE" -> {
                action.setStatus("IGNORED");
                action.setActive(false);
            }
            case "RESTORE" -> {
                action.setStatus("OPEN");
                action.setActive(true);
                priorityService.applyEffectiveBand(action);
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Unknown operation");
        }
        action.setVersion(action.getVersion() + 1);
        action.setUpdatedAt(Instant.now());
        actionItemRepository.save(action);
        return toActionDetail(action);
    }

    private ActionItemEntity load(UUID actionId) {
        return actionItemRepository.findById(actionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACTION_NOT_FOUND", "Action not found"));
    }

    private Map<String, Object> toActionDetail(ActionItemEntity a) {
        Map<String, Object> m = toActionSummary(a);
        m.put("active", a.isActive());
        m.put("createdAt", a.getCreatedAt());
        m.put("updatedAt", a.getUpdatedAt());
        m.put("prioritySortKey", a.getPrioritySortKey());
        m.put("priorityEvidence", priorityService.explain(a));
        return m;
    }

    private Map<String, Object> toActionSummary(ActionItemEntity a) {
        Map<String, Object> target = new HashMap<>();
        target.put("type", a.getTargetType());
        target.put("id", a.getTargetId());
        target.put("routeHint", a.getRouteHint());
        target.put("focusKey", a.getFocusKey());
        Map<String, Object> m = new HashMap<>();
        m.put("actionId", a.getActionId());
        m.put("sourceDomain", a.getSourceDomain());
        m.put("actionKind", a.getActionKind());
        m.put("title", a.getTitle());
        m.put("status", a.getStatus());
        m.put("priorityBand", a.getPriorityBand());
        m.put("deadline", a.getDeadline());
        m.put("pinned", a.isPinned());
        m.put("targetRef", target);
        m.put("primaryReason", a.getPrimaryReason());
        m.put("version", a.getVersion());
        return m;
    }

    private Map<String, Object> toJobSummary(CanonicalJobEntity j) {
        Map<String, Object> m = new HashMap<>();
        m.put("jobId", j.getJobId());
        m.put("title", j.getTitle());
        m.put("company", j.getCompany());
        m.put("location", j.getLocation());
        m.put("jobStatus", j.getJobStatus());
        m.put("validityStatus", j.getValidityStatus());
        m.put("gateStatus", j.getGateStatus());
        m.put("rankTier", j.getRankTier());
        m.put("hiddenByDefault", j.isHiddenByDefault());
        m.put("lastSeenAt", j.getLastSeenAt());
        m.put("hasPendingDuplicate", j.isHasPendingDuplicate());
        m.put("preferredSourceCode", j.getPreferredSourceCode());
        m.put("version", j.getVersion());
        return m;
    }
}
