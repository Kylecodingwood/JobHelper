package com.jobhelper.roadmap.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.roadmap.infrastructure.RoadmapDocumentEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapDocumentRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapFolderEntity;
import com.jobhelper.shared.web.ApiException;

@Service
public class RoadmapDocumentService {
    private final RoadmapDocumentRepository repository;
    private final RoadmapFolderService folderService;

    public RoadmapDocumentService(RoadmapDocumentRepository repository, RoadmapFolderService folderService) {
        this.repository = repository;
        this.folderService = folderService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByFolder(UUID folderId) {
        return repository.findByFolderIdOrderBySortOrderAscUpdatedAtDesc(folderId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String title = str(body == null ? null : body.get("title"), "Untitled");
        UUID folderId = folderService.resolveFolderId(parseUuid(body == null ? null : body.get("folderId")));
        RoadmapFolderEntity folder = folderService.requireFolder(folderId);
        requireKind(folder, RoadmapFolderService.KIND_DOCUMENT);

        Instant now = Instant.now();
        RoadmapDocumentEntity d = new RoadmapDocumentEntity();
        d.setDocumentId(UUID.randomUUID());
        d.setFolderId(folderId);
        d.setTitle(title == null || title.isBlank() ? "Untitled" : title.trim());
        d.setBodyHtml(str(body == null ? null : body.get("bodyHtml"), ""));
        d.setSortOrder((int) repository.countByFolderId(folderId));
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        repository.save(d);
        return toDto(d);
    }

    @Transactional
    public Map<String, Object> patch(UUID documentId, Map<String, Object> body) {
        RoadmapDocumentEntity d = load(documentId);
        if (body != null) {
            if (body.containsKey("title")) {
                String title = str(body.get("title"), null);
                if (title == null || title.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "title required");
                }
                d.setTitle(title.trim());
            }
            if (body.containsKey("bodyHtml")) {
                d.setBodyHtml(str(body.get("bodyHtml"), ""));
            }
            if (body.containsKey("folderId")) {
                UUID folderId = parseUuid(body.get("folderId"));
                if (folderId == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "folderId required");
                }
                RoadmapFolderEntity folder = folderService.requireFolder(folderId);
                requireKind(folder, RoadmapFolderService.KIND_DOCUMENT);
                d.setFolderId(folderId);
            }
        }
        d.setUpdatedAt(Instant.now());
        repository.save(d);
        return toDto(d);
    }

    @Transactional
    public void delete(UUID documentId) {
        if (!repository.existsById(documentId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found");
        }
        repository.deleteById(documentId);
    }

    private void requireKind(RoadmapFolderEntity folder, String kind) {
        if (!kind.equals(folder.getKind())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FOLDER_KIND", "Folder kind must be " + kind);
        }
    }

    private RoadmapDocumentEntity load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found"));
    }

    private Map<String, Object> toDto(RoadmapDocumentEntity d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("documentId", d.getDocumentId());
        m.put("folderId", d.getFolderId());
        m.put("title", d.getTitle());
        m.put("bodyHtml", d.getBodyHtml() == null ? "" : d.getBodyHtml());
        m.put("sortOrder", d.getSortOrder());
        m.put("updatedAt", d.getUpdatedAt());
        m.put("createdAt", d.getCreatedAt());
        return m;
    }

    private String str(Object v, String fallback) {
        if (v == null) {
            return fallback;
        }
        String s = String.valueOf(v);
        return "null".equals(s) ? fallback : s;
    }

    private UUID parseUuid(Object v) {
        if (v == null || String.valueOf(v).isBlank() || "null".equals(String.valueOf(v))) {
            return null;
        }
        return UUID.fromString(String.valueOf(v));
    }
}
