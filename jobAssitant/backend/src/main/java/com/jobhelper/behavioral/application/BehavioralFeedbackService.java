package com.jobhelper.behavioral.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.behavioral.infrastructure.BhvAnswerEntity;
import com.jobhelper.behavioral.infrastructure.BhvAnswerFeedbackEntity;
import com.jobhelper.behavioral.infrastructure.BhvAnswerFeedbackItemEntity;
import com.jobhelper.behavioral.infrastructure.BhvAnswerFeedbackItemRepository;
import com.jobhelper.behavioral.infrastructure.BhvAnswerFeedbackRepository;
import com.jobhelper.behavioral.infrastructure.BhvAnswerRepository;
import com.jobhelper.behavioral.infrastructure.BhvAnswerVersionEntity;
import com.jobhelper.behavioral.infrastructure.BhvAnswerVersionEvidenceEntity;
import com.jobhelper.behavioral.infrastructure.BhvAnswerVersionEvidenceRepository;
import com.jobhelper.behavioral.infrastructure.BhvStarEvidenceRevisionEntity;
import com.jobhelper.shared.web.ApiException;

@Service
public class BehavioralFeedbackService {
    private static final Set<String> ALLOWED_DECISIONS = Set.of("ACCEPT", "REJECT", "DEFER");

    private final BhvAnswerFeedbackRepository feedbackRepository;
    private final BhvAnswerFeedbackItemRepository itemRepository;
    private final BhvAnswerRepository answerRepository;
    private final BhvAnswerVersionEvidenceRepository versionEvidenceRepository;
    private final BehavioralAnswerService answerService;
    private final BehavioralEvidenceService evidenceService;
    private final LocalFeedbackService localFeedbackService;

    public BehavioralFeedbackService(
            BhvAnswerFeedbackRepository feedbackRepository,
            BhvAnswerFeedbackItemRepository itemRepository,
            BhvAnswerRepository answerRepository,
            BhvAnswerVersionEvidenceRepository versionEvidenceRepository,
            BehavioralAnswerService answerService,
            BehavioralEvidenceService evidenceService,
            LocalFeedbackService localFeedbackService) {
        this.feedbackRepository = feedbackRepository;
        this.itemRepository = itemRepository;
        this.answerRepository = answerRepository;
        this.versionEvidenceRepository = versionEvidenceRepository;
        this.answerService = answerService;
        this.evidenceService = evidenceService;
        this.localFeedbackService = localFeedbackService;
    }

    @Transactional
    public Map<String, Object> runLocalFeedback(UUID answerId, UUID versionId) {
        BhvAnswerVersionEntity version = answerService.loadVersion(answerId, versionId);
        List<BhvStarEvidenceRevisionEntity> revisions = versionEvidenceRepository.findByVersionId(versionId).stream()
                .map(BhvAnswerVersionEvidenceEntity::getEvidenceRevisionId)
                .map(evidenceService::loadRevision)
                .toList();

        List<LocalFeedbackService.FeedbackDraft> drafts = localFeedbackService.evaluate(version.getBodyText(), revisions);

        Instant now = Instant.now();
        UUID feedbackId = UUID.randomUUID();

        BhvAnswerFeedbackEntity feedback = new BhvAnswerFeedbackEntity();
        feedback.setFeedbackId(feedbackId);
        feedback.setAnswerVersionId(versionId);
        feedback.setRuleVersion(BehavioralSupport.RULE_VERSION);
        feedback.setCreatedAt(now);
        feedbackRepository.save(feedback);

        for (LocalFeedbackService.FeedbackDraft draft : drafts) {
            BhvAnswerFeedbackItemEntity item = new BhvAnswerFeedbackItemEntity();
            item.setItemId(UUID.randomUUID());
            item.setFeedbackId(feedbackId);
            item.setDimension(draft.dimension());
            item.setRuleId(draft.ruleId());
            item.setTargetQuote(draft.targetQuote());
            item.setIssue(draft.issue());
            item.setRationale(draft.rationale());
            item.setSuggestion(draft.suggestion());
            item.setOrigin(BehavioralSupport.ORIGIN_DETERMINISTIC);
            itemRepository.save(item);
        }

        BhvAnswerEntity answer = answerService.load(answerId);
        answer.setStatus(BehavioralSupport.ANSWER_STATUS_FEEDBACK_AVAILABLE);
        answer.setUpdatedAt(now);
        answerRepository.save(answer);

        return toDto(feedback);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(UUID feedbackId) {
        return toDto(load(feedbackId));
    }

    @Transactional
    public Map<String, Object> decide(UUID feedbackId, UUID itemId, Map<String, Object> body) {
        BhvAnswerFeedbackEntity feedback = load(feedbackId);
        BhvAnswerFeedbackItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ANSWER_NOT_FOUND", "Feedback item not found"));
        if (!feedbackId.equals(item.getFeedbackId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ANSWER_NOT_FOUND", "Feedback item not found");
        }

        String decision = BehavioralSupport.normalizeDecision(BehavioralSupport.readString(body, "decision"));
        if (decision == null || !ALLOWED_DECISIONS.contains(decision)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "decision must be ACCEPT, REJECT, or DEFER");
        }

        item.setDecision(decision);
        item.setDecidedAt(Instant.now());
        itemRepository.save(item);
        return itemToDto(item);
    }

    private BhvAnswerFeedbackEntity load(UUID feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ANSWER_NOT_FOUND", "Feedback not found"));
    }

    private Map<String, Object> toDto(BhvAnswerFeedbackEntity feedback) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("feedbackId", feedback.getFeedbackId());
        m.put("answerVersionId", feedback.getAnswerVersionId());
        m.put("ruleVersion", feedback.getRuleVersion());
        m.put("createdAt", feedback.getCreatedAt());
        m.put("items", itemRepository.findByFeedbackIdOrderByItemIdAsc(feedback.getFeedbackId()).stream()
                .map(this::itemToDto)
                .toList());
        return m;
    }

    private Map<String, Object> itemToDto(BhvAnswerFeedbackItemEntity item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("itemId", item.getItemId());
        m.put("feedbackId", item.getFeedbackId());
        m.put("dimension", item.getDimension());
        m.put("ruleId", item.getRuleId());
        m.put("targetQuote", item.getTargetQuote());
        m.put("issue", item.getIssue());
        m.put("rationale", item.getRationale());
        m.put("suggestion", item.getSuggestion());
        m.put("origin", item.getOrigin());
        m.put("decision", item.getDecision());
        m.put("decidedAt", item.getDecidedAt());
        return m;
    }
}
