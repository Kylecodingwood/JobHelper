package com.jobhelper.action.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.action.infrastructure.ActionItemEntity;
import com.jobhelper.action.infrastructure.ActionItemRepository;
import com.jobhelper.cv.infrastructure.CvDocumentRepository;
import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.job.infrastructure.CanonicalJobRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapCompanyEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapCompanyRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapFolderRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapTodoEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapTodoRepository;

/**
 * Curates Home「今日优先」top slots: review jobs → advance apply → gap/company.
 */
@Service
public class HomeTodayPriorityService {

    public static final UUID SLOT_REVIEW = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0001");
    public static final UUID SLOT_ADVANCE = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0002");
    public static final UUID SLOT_GAP = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0003");

    private final ActionItemRepository actionItemRepository;
    private final CanonicalJobRepository jobRepository;
    private final RoadmapTodoRepository todoRepository;
    private final RoadmapFolderRepository folderRepository;
    private final RoadmapCompanyRepository companyRepository;
    private final CvDocumentRepository cvDocumentRepository;

    public HomeTodayPriorityService(
            ActionItemRepository actionItemRepository,
            CanonicalJobRepository jobRepository,
            RoadmapTodoRepository todoRepository,
            RoadmapFolderRepository folderRepository,
            RoadmapCompanyRepository companyRepository,
            CvDocumentRepository cvDocumentRepository) {
        this.actionItemRepository = actionItemRepository;
        this.jobRepository = jobRepository;
        this.todoRepository = todoRepository;
        this.folderRepository = folderRepository;
        this.companyRepository = companyRepository;
        this.cvDocumentRepository = cvDocumentRepository;
    }

    @Transactional
    public void refresh() {
        deactivateNoise();
        upsertReviewSlot();
        upsertAdvanceSlot();
        upsertGapSlot();
    }

    private void deactivateNoise() {
        Instant now = Instant.now();
        for (ActionItemEntity a : actionItemRepository.findByActiveTrueAndActionKindIn(
                List.of("COMPLETE_ROADMAP_TASK", "SYNC_JOBS"))) {
            if (a.isPinned()) {
                continue;
            }
            a.setActive(false);
            a.setStatus("SUPERSEDED");
            a.setUpdatedAt(now);
            a.setVersion(a.getVersion() + 1);
            actionItemRepository.save(a);
        }
        // Demote per-job review cards so they don't crowd the top 3.
        for (ActionItemEntity a : actionItemRepository.findByActiveTrueAndActionKind("REVIEW_NEW_JOB")) {
            if (a.isPinned()) {
                continue;
            }
            a.setPrioritySortKey("9|JOB|" + a.getTargetId());
            a.setPriorityBand("SUGGESTION");
            a.setUpdatedAt(now);
            a.setVersion(a.getVersion() + 1);
            actionItemRepository.save(a);
        }
    }

    private void upsertReviewSlot() {
        long totalNew = jobRepository.countByJobStatusAndHiddenByDefaultFalse("NEW");
        List<CanonicalJobEntity> sample = jobRepository
                .findByJobStatusAndHiddenByDefaultFalseOrderByLastSeenAtDesc("NEW", PageRequest.of(0, 1));
        String title;
        String reason;
        String route = "/jobs?fit=junior";
        if (totalNew == 0) {
            title = "打开 Jobs 查看 inbox";
            reason = "今日优先·岗位动作（暂无 NEW）";
        } else if (totalNew == 1 && !sample.isEmpty()) {
            CanonicalJobEntity j = sample.get(0);
            String company = j.getCompany() == null || j.getCompany().isBlank() ? "未知公司" : j.getCompany();
            String jobTitle = j.getTitle() == null ? "职位" : j.getTitle();
            title = "审阅新职位：" + company + " — " + jobTitle;
            reason = "今日优先·岗位动作";
            route = "/jobs?jobId=" + j.getJobId();
        } else {
            title = "查看新职位：" + totalNew + " 条待审";
            reason = "今日优先·岗位动作";
        }
        upsertSlot(SLOT_REVIEW, "JOB", "REVIEW_TODAY_JOBS", title, "JOBS_INBOX", route, "today:review",
                "SUGGESTION", reason, "0|TODAY|1");
    }

    private void upsertAdvanceSlot() {
        Optional<RoadmapTodoEntity> todo = openTodosPreferDue().stream().findFirst();
        if (todo.isPresent()) {
            RoadmapTodoEntity t = todo.get();
            Instant deadline = t.getDueAt();
            String band = deadline != null ? "DEADLINE" : "SUGGESTION";
            String sort = deadline != null ? "0|TODAY|2|DEAD|" + deadline : "0|TODAY|2";
            upsertSlotWithDeadline(
                    SLOT_ADVANCE,
                    "ROADMAP",
                    "ADVANCE_TODAY_TODO",
                    "推进任务：" + t.getName(),
                    "ROADMAP_TODO",
                    t.getTodoId(),
                    "/roadmap?folderId=" + t.getFolderId(),
                    "today:advance",
                    band,
                    deadline != null ? "今日优先·投递闭环（有截止）" : "今日优先·投递闭环",
                    sort,
                    deadline);
            return;
        }
        long shortlisted = jobRepository.countByJobStatusAndHiddenByDefaultFalse("SHORTLISTED");
        if (shortlisted > 0) {
            upsertSlot(SLOT_ADVANCE, "JOB", "ADVANCE_SHORTLIST",
                    "推进 Shortlist：" + shortlisted + " 家待投",
                    "JOBS_SHORTLIST", "/jobs", "today:advance",
                    "SUGGESTION", "今日优先·投递闭环", "0|TODAY|2");
            return;
        }
        upsertSlot(SLOT_ADVANCE, "ROADMAP", "ADVANCE_TODAY_TODO",
                "在 Roadmap 写下今日投递",
                "ROADMAP", "/roadmap", "today:advance",
                "SUGGESTION", "今日优先·投递闭环（尚无待办）", "0|TODAY|2");
    }

    private void upsertGapSlot() {
        Optional<RoadmapCompanyEntity> watching = companyRepository.findByStatusOrderByUpdatedAtDesc("watching")
                .stream()
                .findFirst();
        if (watching.isPresent()) {
            RoadmapCompanyEntity c = watching.get();
            upsertSlotWithDeadline(
                    SLOT_GAP,
                    "ROADMAP",
                    "FOLLOW_COMPANY",
                    "跟进公司：" + c.getCompanyName() + "（想投）",
                    "ROADMAP_COMPANY",
                    c.getCompanyId(),
                    "/roadmap?folderId=" + c.getFolderId(),
                    "today:gap",
                    "SUGGESTION",
                    "今日优先·公司跟进",
                    "0|TODAY|3",
                    null);
            return;
        }
        if (cvDocumentRepository.count() == 0) {
            upsertSlot(SLOT_GAP, "CV", "UPLOAD_CV",
                    "上传或完善 CV",
                    "CV", "/cv", "today:gap",
                    "SUGGESTION", "今日优先·材料缺口", "0|TODAY|3");
            return;
        }
        boolean hasCompanyFolder = folderRepository.findAllByOrderBySortOrderAscUpdatedAtAsc().stream()
                .anyMatch(f -> "companytracker".equals(f.getKind()));
        if (!hasCompanyFolder) {
            upsertSlot(SLOT_GAP, "ROADMAP", "START_COMPANYTRACKER",
                    "新建 CompanyTracker 夹并加目标公司",
                    "ROADMAP", "/roadmap", "today:gap",
                    "SUGGESTION", "今日优先·公司跟进", "0|TODAY|3");
            return;
        }
        upsertSlot(SLOT_GAP, "ROADMAP", "ADD_TARGET_COMPANY",
                "在 CompanyTracker 加 1–2 家目标公司",
                "ROADMAP", "/roadmap", "today:gap",
                "SUGGESTION", "今日优先·公司跟进", "0|TODAY|3");
    }

    private List<RoadmapTodoEntity> openTodosPreferDue() {
        return todoRepository.findByDoneFalseOrderBySortOrderAscUpdatedAtDesc().stream()
                .sorted(Comparator
                        .comparing((RoadmapTodoEntity t) -> t.getDueAt() == null)
                        .thenComparing(t -> t.getDueAt() == null ? Instant.MAX : t.getDueAt())
                        .thenComparingInt(RoadmapTodoEntity::getSortOrder))
                .toList();
    }

    private void upsertSlot(
            UUID slotId,
            String sourceDomain,
            String actionKind,
            String title,
            String targetType,
            String route,
            String focusKey,
            String band,
            String reason,
            String sortKey) {
        upsertSlotWithDeadline(slotId, sourceDomain, actionKind, title, targetType, slotId, route, focusKey, band, reason, sortKey, null);
    }

    private void upsertSlotWithDeadline(
            UUID slotId,
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
        Instant now = Instant.now();
        ActionItemEntity a = actionItemRepository.findById(slotId).orElseGet(ActionItemEntity::new);
        boolean isNew = a.getActionId() == null;
        if (isNew) {
            a.setActionId(slotId);
            a.setCreatedAt(now);
            a.setVersion(1);
            a.setPinned(false);
        } else {
            a.setVersion(a.getVersion() + 1);
        }
        a.setSourceDomain(sourceDomain);
        a.setActionKind(actionKind);
        a.setTitle(title);
        a.setStatus("OPEN");
        a.setActive(true);
        a.setPriorityBand(band);
        a.setPrioritySortKey(sortKey);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setRouteHint(route);
        a.setFocusKey(focusKey);
        a.setPrimaryReason(reason);
        a.setDeadline(deadline);
        a.setUpdatedAt(now);
        actionItemRepository.save(a);
    }
}
