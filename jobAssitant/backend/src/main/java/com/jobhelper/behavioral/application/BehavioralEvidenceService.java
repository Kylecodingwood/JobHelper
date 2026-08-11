package com.jobhelper.behavioral.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.behavioral.infrastructure.BhvStarEvidenceEntity;
import com.jobhelper.behavioral.infrastructure.BhvStarEvidenceRepository;
import com.jobhelper.behavioral.infrastructure.BhvStarEvidenceRevisionEntity;
import com.jobhelper.behavioral.infrastructure.BhvStarEvidenceRevisionRepository;
import com.jobhelper.shared.web.ApiException;

@Service
public class BehavioralEvidenceService {
    private final BhvStarEvidenceRepository evidenceRepository;
    private final BhvStarEvidenceRevisionRepository revisionRepository;

    public BehavioralEvidenceService(
            BhvStarEvidenceRepository evidenceRepository,
            BhvStarEvidenceRevisionRepository revisionRepository) {
        this.evidenceRepository = evidenceRepository;
        this.revisionRepository = revisionRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list() {
        List<Map<String, Object>> items = evidenceRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return Map.of("items", items);
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String title = BehavioralSupport.readString(body, "title");
        if (BehavioralSupport.isBlank(title)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "title required");
        }

        String situation = BehavioralSupport.readString(body, "situation");
        String task = BehavioralSupport.readString(body, "task");
        String action = BehavioralSupport.readString(body, "action");
        String result = BehavioralSupport.readString(body, "result");
        List<String> tags = BehavioralSupport.readStringList(body, "competencyTags");

        Instant now = Instant.now();
        UUID evidenceId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        BhvStarEvidenceEntity evidence = new BhvStarEvidenceEntity();
        evidence.setEvidenceId(evidenceId);
        evidence.setTitle(title.trim());
        evidence.setStatus(BehavioralSupport.evidenceStatus(situation, task, action, result));
        evidence.setCreatedAt(now);
        evidence.setUpdatedAt(now);
        evidenceRepository.save(evidence);

        BhvStarEvidenceRevisionEntity revision = newRevision(
                revisionId, evidenceId, null, situation, task, action, result, tags, now);
        revisionRepository.save(revision);

        evidence.setCurrentRevisionId(revisionId);
        evidenceRepository.save(evidence);
        return toDto(evidence);
    }

    @Transactional
    public Map<String, Object> update(UUID evidenceId, Map<String, Object> body) {
        BhvStarEvidenceEntity evidence = load(evidenceId);
        BhvStarEvidenceRevisionEntity current = loadRevision(evidence.getCurrentRevisionId());

        String title = BehavioralSupport.readString(body, "title");
        if (title != null && !title.isBlank()) {
            evidence.setTitle(title.trim());
        }

        String situation = coalesce(body, "situation", current.getSituation());
        String task = coalesce(body, "task", current.getTask());
        String action = coalesce(body, "action", current.getAction());
        String result = coalesce(body, "result", current.getResult());
        List<String> tags = body.containsKey("competencyTags")
                ? BehavioralSupport.readStringList(body, "competencyTags")
                : BehavioralSupport.decodeTags(current.getCompetencyTags());

        Instant now = Instant.now();
        UUID revisionId = UUID.randomUUID();
        BhvStarEvidenceRevisionEntity revision = newRevision(
                revisionId,
                evidenceId,
                current.getRevisionId(),
                situation,
                task,
                action,
                result,
                tags,
                now);

        evidence.setCurrentRevisionId(revisionId);
        evidence.setStatus(BehavioralSupport.evidenceStatus(situation, task, action, result));
        evidence.setUpdatedAt(now);

        revisionRepository.save(revision);
        evidenceRepository.save(evidence);
        return toDto(evidence);
    }

    @Transactional
    public void delete(UUID evidenceId) {
        BhvStarEvidenceEntity evidence = load(evidenceId);
        evidenceRepository.delete(evidence);
    }

    @Transactional(readOnly = true)
    public BhvStarEvidenceEntity load(UUID evidenceId) {
        return evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EVIDENCE_NOT_FOUND", "Evidence not found"));
    }

    @Transactional(readOnly = true)
    public BhvStarEvidenceRevisionEntity loadRevision(UUID revisionId) {
        return revisionRepository.findById(revisionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EVIDENCE_NOT_FOUND", "Evidence revision not found"));
    }

    @Transactional(readOnly = true)
    public BhvStarEvidenceRevisionEntity loadCurrentRevision(UUID evidenceId) {
        BhvStarEvidenceEntity evidence = load(evidenceId);
        return loadRevision(evidence.getCurrentRevisionId());
    }

    private String coalesce(Map<String, Object> body, String key, String fallback) {
        if (!body.containsKey(key)) {
            return fallback;
        }
        String value = BehavioralSupport.readString(body, key);
        return value == null ? fallback : value;
    }

    private BhvStarEvidenceRevisionEntity newRevision(
            UUID revisionId,
            UUID evidenceId,
            UUID parentRevisionId,
            String situation,
            String task,
            String action,
            String result,
            List<String> tags,
            Instant createdAt) {
        BhvStarEvidenceRevisionEntity revision = new BhvStarEvidenceRevisionEntity();
        revision.setRevisionId(revisionId);
        revision.setEvidenceId(evidenceId);
        revision.setParentRevisionId(parentRevisionId);
        revision.setSituation(situation);
        revision.setTask(task);
        revision.setAction(action);
        revision.setResult(result);
        revision.setCompetencyTags(BehavioralSupport.encodeTags(tags));
        revision.setCreatedAt(createdAt);
        return revision;
    }

    private Map<String, Object> toDto(BhvStarEvidenceEntity evidence) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("evidenceId", evidence.getEvidenceId());
        m.put("title", evidence.getTitle());
        m.put("status", evidence.getStatus());
        m.put("currentRevisionId", evidence.getCurrentRevisionId());
        m.put("createdAt", evidence.getCreatedAt());
        m.put("updatedAt", evidence.getUpdatedAt());
        if (evidence.getCurrentRevisionId() != null) {
            revisionRepository.findById(evidence.getCurrentRevisionId())
                    .ifPresent(revision -> m.put("currentRevision", revisionToDto(revision)));
        }
        return m;
    }

    static Map<String, Object> revisionToDto(BhvStarEvidenceRevisionEntity revision) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("revisionId", revision.getRevisionId());
        m.put("evidenceId", revision.getEvidenceId());
        m.put("parentRevisionId", revision.getParentRevisionId());
        m.put("situation", revision.getSituation());
        m.put("task", revision.getTask());
        m.put("action", revision.getAction());
        m.put("result", revision.getResult());
        m.put("competencyTags", BehavioralSupport.decodeTags(revision.getCompetencyTags()));
        m.put("createdAt", revision.getCreatedAt());
        return m;
    }
}
