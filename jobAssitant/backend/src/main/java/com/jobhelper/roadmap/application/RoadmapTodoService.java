package com.jobhelper.roadmap.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.roadmap.infrastructure.RoadmapTodoEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapTodoRepository;
import com.jobhelper.shared.outbox.OutboxService;
import com.jobhelper.shared.web.ApiException;

@Service
public class RoadmapTodoService {
    private final RoadmapTodoRepository repository;
    private final RoadmapFolderService folderService;
    private final RoadmapCompanyService companyService;
    private final OutboxService outboxService;

    public RoadmapTodoService(
            RoadmapTodoRepository repository,
            RoadmapFolderService folderService,
            RoadmapCompanyService companyService,
            OutboxService outboxService) {
        this.repository = repository;
        this.folderService = folderService;
        this.companyService = companyService;
        this.outboxService = outboxService;
    }

    @Transactional
    public Map<String, Object> list(UUID folderIdOrNull) {
        UUID folderId = folderService.resolveFolderId(folderIdOrNull);
        var folder = folderService.requireFolder(folderId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("folderId", folderId);
        out.put("kind", folder.getKind() == null ? RoadmapFolderService.KIND_TODOLIST : folder.getKind());
        out.put("folders", folderService.listFolders());
        if (RoadmapFolderService.KIND_COMPANYTRACKER.equals(folder.getKind())) {
            out.put("todos", List.of());
            out.put("companies", companyService.listByFolder(folderId));
        } else {
            List<Map<String, Object>> todos = repository
                    .findByFolderIdOrderByDoneAscSortOrderAscUpdatedAtDesc(folderId).stream()
                    .map(this::toDto)
                    .toList();
            out.put("todos", todos);
            out.put("companies", List.of());
        }
        return out;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String name = str(body == null ? null : body.get("name"), null);
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name required");
        }
        UUID folderId = folderService.resolveFolderId(parseUuid(body == null ? null : body.get("folderId")));
        var folder = folderService.requireFolder(folderId);
        if (RoadmapFolderService.KIND_COMPANYTRACKER.equals(folder.getKind())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FOLDER_KIND",
                    "Use company endpoints for companytracker folders");
        }
        Instant now = Instant.now();
        RoadmapTodoEntity t = new RoadmapTodoEntity();
        t.setTodoId(UUID.randomUUID());
        t.setFolderId(folderId);
        t.setName(name.trim());
        t.setDueAt(parseInstant(body.get("dueAt")));
        t.setComment(str(body.get("comment"), null));
        t.setDone(false);
        t.setSortOrder((int) repository.countByFolderId(folderId));
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        repository.save(t);
        outboxService.append("ROADMAP", "TODO_CREATED", "TODO", t.getTodoId(),
                "{\"name\":\"" + escape(t.getName()) + "\"}");
        return toDto(t);
    }

    @Transactional
    public Map<String, Object> patch(UUID todoId, Map<String, Object> body) {
        RoadmapTodoEntity t = load(todoId);
        if (body != null) {
            if (body.containsKey("name")) {
                String name = str(body.get("name"), null);
                if (name == null || name.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name required");
                }
                t.setName(name.trim());
            }
            if (body.containsKey("dueAt")) {
                t.setDueAt(parseInstant(body.get("dueAt")));
            }
            if (body.containsKey("comment")) {
                t.setComment(str(body.get("comment"), null));
            }
            if (body.containsKey("done")) {
                t.setDone(Boolean.TRUE.equals(body.get("done"))
                        || "true".equalsIgnoreCase(String.valueOf(body.get("done"))));
            }
            if (body.containsKey("folderId")) {
                UUID folderId = parseUuid(body.get("folderId"));
                if (folderId == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "folderId required");
                }
                folderService.requireFolder(folderId);
                t.setFolderId(folderId);
            }
        }
        t.setUpdatedAt(Instant.now());
        repository.save(t);
        return toDto(t);
    }

    @Transactional
    public Map<String, Object> toggle(UUID todoId) {
        RoadmapTodoEntity t = load(todoId);
        t.setDone(!t.isDone());
        t.setUpdatedAt(Instant.now());
        repository.save(t);
        if (t.isDone()) {
            outboxService.append("ROADMAP", "TODO_COMPLETED", "TODO", t.getTodoId(), "{}");
        }
        return toDto(t);
    }

    @Transactional
    public void delete(UUID todoId) {
        if (!repository.existsById(todoId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "Todo not found");
        }
        repository.deleteById(todoId);
    }

    private RoadmapTodoEntity load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "Todo not found"));
    }

    private Map<String, Object> toDto(RoadmapTodoEntity t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todoId", t.getTodoId());
        m.put("folderId", t.getFolderId());
        m.put("name", t.getName());
        m.put("dueAt", t.getDueAt());
        m.put("comment", t.getComment());
        m.put("done", t.isDone());
        m.put("sortOrder", t.getSortOrder());
        m.put("updatedAt", t.getUpdatedAt());
        return m;
    }

    private String str(Object v, String fallback) {
        if (v == null) {
            return fallback;
        }
        String s = String.valueOf(v);
        return s.isBlank() ? fallback : s;
    }

    private Instant parseInstant(Object v) {
        if (v == null || String.valueOf(v).isBlank() || "null".equals(String.valueOf(v))) {
            return null;
        }
        return Instant.parse(String.valueOf(v));
    }

    private UUID parseUuid(Object v) {
        if (v == null || String.valueOf(v).isBlank() || "null".equals(String.valueOf(v))) {
            return null;
        }
        return UUID.fromString(String.valueOf(v));
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
