package com.jobhelper.behavioral.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.behavioral.infrastructure.BhvAnswerEntity;
import com.jobhelper.behavioral.infrastructure.BhvAnswerRepository;
import com.jobhelper.behavioral.infrastructure.BhvAnswerVersionEntity;
import com.jobhelper.behavioral.infrastructure.BhvAnswerVersionEvidenceEntity;
import com.jobhelper.behavioral.infrastructure.BhvAnswerVersionEvidenceRepository;
import com.jobhelper.behavioral.infrastructure.BhvAnswerVersionRepository;
import com.jobhelper.behavioral.infrastructure.BhvQuestionEntity;
import com.jobhelper.behavioral.infrastructure.BhvStarEvidenceRevisionEntity;
import com.jobhelper.shared.web.ApiException;

@Service
public class BehavioralAnswerService {
    private final BhvAnswerRepository answerRepository;
    private final BhvAnswerVersionRepository versionRepository;
    private final BhvAnswerVersionEvidenceRepository versionEvidenceRepository;
    private final BehavioralQuestionService questionService;
    private final BehavioralEvidenceService evidenceService;

    public BehavioralAnswerService(
            BhvAnswerRepository answerRepository,
            BhvAnswerVersionRepository versionRepository,
            BhvAnswerVersionEvidenceRepository versionEvidenceRepository,
            BehavioralQuestionService questionService,
            BehavioralEvidenceService evidenceService) {
        this.answerRepository = answerRepository;
        this.versionRepository = versionRepository;
        this.versionEvidenceRepository = versionEvidenceRepository;
        this.questionService = questionService;
        this.evidenceService = evidenceService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(UUID questionId) {
        List<BhvAnswerEntity> answers = questionId == null
                ? answerRepository.findAll()
                : answerRepository.findByQuestionIdOrderByCreatedAtDesc(questionId);
        List<Map<String, Object>> items = answers.stream()
                .map(this::toSummaryDto)
                .toList();
        return Map.of("items", items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(UUID answerId) {
        BhvAnswerEntity answer = load(answerId);
        Map<String, Object> dto = toSummaryDto(answer);
        if (answer.getCurrentVersionId() != null) {
            versionRepository.findById(answer.getCurrentVersionId())
                    .ifPresent(version -> dto.put("currentVersion", versionToDto(version)));
        }
        return dto;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String questionIdRaw = BehavioralSupport.readString(body, "questionId");
        String bodyText = BehavioralSupport.readString(body, "bodyText");
        if (BehavioralSupport.isBlank(questionIdRaw) || BehavioralSupport.isBlank(bodyText)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "questionId and bodyText required");
        }

        UUID questionId = UUID.fromString(questionIdRaw);
        BhvQuestionEntity question = questionService.load(questionId);
        List<UUID> evidenceIds = BehavioralSupport.readUuidList(body, "evidenceIds");

        Instant now = Instant.now();
        UUID answerId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        BhvAnswerEntity answer = new BhvAnswerEntity();
        answer.setAnswerId(answerId);
        answer.setQuestionId(questionId);
        answer.setStatus(BehavioralSupport.ANSWER_STATUS_DRAFT);
        answer.setCreatedAt(now);
        answer.setUpdatedAt(now);

        BhvAnswerVersionEntity version = newVersion(
                versionId,
                answerId,
                null,
                1,
                question.getText(),
                bodyText.trim(),
                now);

        answer.setCurrentVersionId(versionId);

        answerRepository.save(answer);
        versionRepository.save(version);
        linkEvidence(versionId, evidenceIds);

        Map<String, Object> dto = toSummaryDto(answer);
        dto.put("currentVersion", versionToDto(version));
        return dto;
    }

    @Transactional
    public Map<String, Object> createVersion(UUID answerId, Map<String, Object> body) {
        BhvAnswerEntity answer = load(answerId);
        String bodyText = BehavioralSupport.readString(body, "bodyText");
        if (BehavioralSupport.isBlank(bodyText)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "bodyText required");
        }

        BhvQuestionEntity question = questionService.load(answer.getQuestionId());
        BhvAnswerVersionEntity latest = versionRepository.findTopByAnswerIdOrderByVersionNumberDesc(answerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ANSWER_NOT_FOUND", "Answer has no versions"));

        UUID parentVersionId = latest.getVersionId();
        String parentRaw = BehavioralSupport.readString(body, "parentVersionId");
        if (parentRaw != null && !parentRaw.isBlank()) {
            UUID requestedParent = UUID.fromString(parentRaw);
            versionRepository.findById(requestedParent)
                    .filter(v -> answerId.equals(v.getAnswerId()))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "parentVersionId invalid"));
            parentVersionId = requestedParent;
        }

        List<UUID> evidenceIds = body.containsKey("evidenceIds")
                ? BehavioralSupport.readUuidList(body, "evidenceIds")
                : currentEvidenceIds(parentVersionId);

        Instant now = Instant.now();
        UUID versionId = UUID.randomUUID();
        int versionNumber = latest.getVersionNumber() + 1;

        BhvAnswerVersionEntity version = newVersion(
                versionId,
                answerId,
                parentVersionId,
                versionNumber,
                question.getText(),
                bodyText.trim(),
                now);

        answer.setCurrentVersionId(versionId);
        answer.setUpdatedAt(now);

        versionRepository.save(version);
        answerRepository.save(answer);
        linkEvidence(versionId, evidenceIds);

        Map<String, Object> dto = versionToDto(version);
        dto.put("answerId", answerId);
        return dto;
    }

    @Transactional(readOnly = true)
    public BhvAnswerEntity load(UUID answerId) {
        return answerRepository.findById(answerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ANSWER_NOT_FOUND", "Answer not found"));
    }

    @Transactional(readOnly = true)
    public BhvAnswerVersionEntity loadVersion(UUID answerId, UUID versionId) {
        BhvAnswerVersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ANSWER_NOT_FOUND", "Answer version not found"));
        if (!answerId.equals(version.getAnswerId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ANSWER_NOT_FOUND", "Answer version not found for answer");
        }
        return version;
    }

    private BhvAnswerVersionEntity newVersion(
            UUID versionId,
            UUID answerId,
            UUID parentVersionId,
            int versionNumber,
            String questionTextSnapshot,
            String bodyText,
            Instant createdAt) {
        BhvAnswerVersionEntity version = new BhvAnswerVersionEntity();
        version.setVersionId(versionId);
        version.setAnswerId(answerId);
        version.setParentVersionId(parentVersionId);
        version.setVersionNumber(versionNumber);
        version.setQuestionTextSnapshot(questionTextSnapshot);
        version.setBodyText(bodyText);
        version.setCreatedAt(createdAt);
        return version;
    }

    private void linkEvidence(UUID versionId, List<UUID> evidenceIds) {
        for (UUID evidenceId : evidenceIds) {
            BhvStarEvidenceRevisionEntity revision = evidenceService.loadCurrentRevision(evidenceId);
            versionEvidenceRepository.save(new BhvAnswerVersionEvidenceEntity(versionId, revision.getRevisionId()));
        }
    }

    private List<UUID> currentEvidenceIds(UUID versionId) {
        List<UUID> evidenceIds = new ArrayList<>();
        for (BhvAnswerVersionEvidenceEntity link : versionEvidenceRepository.findByVersionId(versionId)) {
            BhvStarEvidenceRevisionEntity revision = evidenceService.loadRevision(link.getEvidenceRevisionId());
            evidenceIds.add(revision.getEvidenceId());
        }
        return evidenceIds;
    }

    private Map<String, Object> toSummaryDto(BhvAnswerEntity answer) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("answerId", answer.getAnswerId());
        m.put("questionId", answer.getQuestionId());
        m.put("currentVersionId", answer.getCurrentVersionId());
        m.put("status", answer.getStatus());
        m.put("createdAt", answer.getCreatedAt());
        m.put("updatedAt", answer.getUpdatedAt());
        return m;
    }

    private Map<String, Object> versionToDto(BhvAnswerVersionEntity version) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("versionId", version.getVersionId());
        m.put("answerId", version.getAnswerId());
        m.put("parentVersionId", version.getParentVersionId());
        m.put("versionNumber", version.getVersionNumber());
        m.put("questionTextSnapshot", version.getQuestionTextSnapshot());
        m.put("bodyText", version.getBodyText());
        m.put("createdAt", version.getCreatedAt());

        List<Map<String, Object>> evidenceLinks = new ArrayList<>();
        for (BhvAnswerVersionEvidenceEntity link : versionEvidenceRepository.findByVersionId(version.getVersionId())) {
            Map<String, Object> linkDto = new LinkedHashMap<>();
            BhvStarEvidenceRevisionEntity revision = evidenceService.loadRevision(link.getEvidenceRevisionId());
            linkDto.put("evidenceRevisionId", link.getEvidenceRevisionId());
            linkDto.put("revision", BehavioralEvidenceService.revisionToDto(revision));
            evidenceLinks.add(linkDto);
        }
        m.put("evidenceLinks", evidenceLinks);
        return m;
    }
}
