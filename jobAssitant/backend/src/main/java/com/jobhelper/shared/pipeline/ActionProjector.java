package com.jobhelper.shared.pipeline;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.action.infrastructure.ActionItemEntity;
import com.jobhelper.action.infrastructure.ActionItemRepository;
import com.jobhelper.job.infrastructure.CanonicalJobEntity;

@Service
public class ActionProjector {
    private final ActionItemRepository actionItemRepository;

    public ActionProjector(ActionItemRepository actionItemRepository) {
        this.actionItemRepository = actionItemRepository;
    }

    @Transactional
    public void onJobCreated(CanonicalJobEntity job) {
        if (!"NEW".equals(job.getJobStatus())) {
            return;
        }
        upsertOpen(
                "JOB",
                "REVIEW_NEW_JOB",
                "审阅新职位：" + job.getTitle(),
                "JOB",
                job.getJobId(),
                "/jobs?jobId=" + job.getJobId(),
                "job:" + job.getJobId(),
                "SUGGESTION",
                "新职位进入 inbox",
                "B|SUGGEST|" + job.getJobId());
        if ("NEEDS_CONFIRMATION".equals(job.getGateStatus())) {
            upsertOpen(
                    "JOB",
                    "RESOLVE_GATE",
                    "确认门槛：" + job.getTitle(),
                    "JOB",
                    job.getJobId(),
                    "/jobs?jobId=" + job.getJobId(),
                    "gate:" + job.getJobId(),
                    "BLOCKER",
                    "Gate 需要确认",
                    "A|BLOCK|" + job.getJobId());
        }
    }

    @Transactional
    public void onRoadmapTaskActionable(UUID taskId, String title, Instant dueAt, String roadmapId) {
        String band = dueAt != null ? "DEADLINE" : "SUGGESTION";
        String sort = dueAt != null ? "0|DEAD|" + dueAt + "|" + taskId : "B|SUGGEST|" + taskId;
        upsertOpenWithDeadline(
                "ROADMAP",
                "COMPLETE_ROADMAP_TASK",
                title,
                "ROADMAP_TASK",
                taskId,
                "/roadmap",
                "task:" + taskId,
                band,
                dueAt != null ? "任务到期日驱动" : "Roadmap 可执行任务",
                sort,
                dueAt);
    }

    @Transactional
    public void onProfileReady() {
        upsertOpenWithDeadline(
                "SYSTEM",
                "SYNC_JOBS",
                "同步职位（FreeHire / JobSpy）",
                "SYSTEM",
                UUID.nameUUIDFromBytes("sync-jobs".getBytes()),
                "/jobs",
                "sync",
                "SUGGESTION",
                "Profile 已就绪，可拉取职位",
                "B|SUGGEST|sync",
                null);
    }

    private void upsertOpen(
            String sourceDomain,
            String actionKind,
            String title,
            String targetType,
            UUID targetId,
            String route,
            String focusKey,
            String band,
            String reason,
            String sortKey) {
        upsertOpenWithDeadline(sourceDomain, actionKind, title, targetType, targetId, route, focusKey, band, reason, sortKey, null);
    }

    private void upsertOpenWithDeadline(
            String sourceDomain,
            String actionKind,
            String title,
            String targetType,
            UUID targetId,
            String route,
            String focusKey,
            String band,
            String reason,
            String sortKey,
            Instant deadline) {
        var existing = actionItemRepository.findAll().stream()
                .filter(a -> a.isActive()
                        && actionKind.equals(a.getActionKind())
                        && targetType.equals(a.getTargetType())
                        && targetId.equals(a.getTargetId()))
                .findFirst();
        Instant now = Instant.now();
        if (existing.isPresent()) {
            ActionItemEntity a = existing.get();
            a.setTitle(title);
            a.setPriorityBand(band);
            a.setPrioritySortKey(sortKey);
            a.setPrimaryReason(reason);
            a.setDeadline(deadline);
            a.setUpdatedAt(now);
            a.setVersion(a.getVersion() + 1);
            actionItemRepository.save(a);
            return;
        }
        ActionItemEntity a = new ActionItemEntity();
        a.setActionId(UUID.randomUUID());
        a.setSourceDomain(sourceDomain);
        a.setActionKind(actionKind);
        a.setTitle(title);
        a.setStatus("OPEN");
        a.setActive(true);
        a.setPinned(false);
        a.setPriorityBand(band);
        a.setPrioritySortKey(sortKey);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setRouteHint(route);
        a.setFocusKey(focusKey);
        a.setPrimaryReason(reason);
        a.setDeadline(deadline);
        a.setVersion(1);
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        actionItemRepository.save(a);
    }
}
