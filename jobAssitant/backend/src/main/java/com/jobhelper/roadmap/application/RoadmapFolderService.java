package com.jobhelper.roadmap.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.roadmap.infrastructure.RoadmapCompanyRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapDocumentRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapFolderEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapFolderRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapTodoRepository;
import com.jobhelper.shared.web.ApiException;

@Service
public class RoadmapFolderService {

    /** Seeded in V13 — existing flat todos land here. */
    public static final UUID DEFAULT_FOLDER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-000000000001");
    public static final String KIND_TODOLIST = "todolist";
    public static final String KIND_COMPANYTRACKER = "companytracker";
    public static final String KIND_DOCUMENT = "document";
    public static final Set<String> KINDS = Set.of(KIND_TODOLIST, KIND_COMPANYTRACKER, KIND_DOCUMENT);

    private final RoadmapFolderRepository folderRepository;
    private final RoadmapTodoRepository todoRepository;
    private final RoadmapCompanyRepository companyRepository;
    private final RoadmapDocumentRepository documentRepository;

    public RoadmapFolderService(
            RoadmapFolderRepository folderRepository,
            RoadmapTodoRepository todoRepository,
            RoadmapCompanyRepository companyRepository,
            RoadmapDocumentRepository documentRepository) {
        this.folderRepository = folderRepository;
        this.todoRepository = todoRepository;
        this.companyRepository = companyRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public List<Map<String, Object>> listFolders() {
        ensureDefaultFolder();
        return folderRepository.findAllByOrderBySortOrderAscUpdatedAtAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String name = str(body == null ? null : body.get("name"), null);
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name required");
        }
        String kind = normalizeKind(body == null ? null : body.get("kind"));
        Instant now = Instant.now();
        RoadmapFolderEntity f = new RoadmapFolderEntity();
        f.setFolderId(UUID.randomUUID());
        f.setName(name.trim());
        f.setKind(kind);
        f.setSortOrder((int) folderRepository.count());
        f.setCreatedAt(now);
        f.setUpdatedAt(now);
        folderRepository.save(f);
        return toDto(f);
    }

    @Transactional
    public Map<String, Object> patch(UUID folderId, Map<String, Object> body) {
        RoadmapFolderEntity f = load(folderId);
        if (body != null && body.containsKey("name")) {
            String name = str(body.get("name"), null);
            if (name == null || name.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name required");
            }
            f.setName(name.trim());
        }
        f.setUpdatedAt(Instant.now());
        folderRepository.save(f);
        return toDto(f);
    }

    @Transactional
    public void delete(UUID folderId) {
        if (folderRepository.count() <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FOLDER_LAST", "Cannot delete the last folder");
        }
        if (!folderRepository.existsById(folderId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FOLDER_NOT_FOUND", "Folder not found");
        }
        // ON DELETE CASCADE removes todos / companies in this folder.
        folderRepository.deleteById(folderId);
    }

    @Transactional(readOnly = true)
    public RoadmapFolderEntity requireFolder(UUID folderId) {
        return load(folderId);
    }

    @Transactional
    public UUID resolveFolderId(UUID folderIdOrNull) {
        ensureDefaultFolder();
        if (folderIdOrNull != null) {
            load(folderIdOrNull);
            return folderIdOrNull;
        }
        return folderRepository.findAllByOrderBySortOrderAscUpdatedAtAsc().stream()
                .findFirst()
                .map(RoadmapFolderEntity::getFolderId)
                .orElse(DEFAULT_FOLDER_ID);
    }

    private void ensureDefaultFolder() {
        if (folderRepository.existsById(DEFAULT_FOLDER_ID)) {
            RoadmapFolderEntity existing = folderRepository.findById(DEFAULT_FOLDER_ID).orElseThrow();
            boolean dirty = false;
            if (existing.getKind() == null || existing.getKind().isBlank()) {
                existing.setKind(KIND_TODOLIST);
                dirty = true;
            }
            if ("默认".equals(existing.getName())) {
                existing.setName("todolist");
                dirty = true;
            }
            if (dirty) {
                existing.setUpdatedAt(Instant.now());
                folderRepository.save(existing);
            }
            return;
        }
        Instant now = Instant.now();
        RoadmapFolderEntity f = new RoadmapFolderEntity();
        f.setFolderId(DEFAULT_FOLDER_ID);
        f.setName("todolist");
        f.setKind(KIND_TODOLIST);
        f.setSortOrder(0);
        f.setCreatedAt(now);
        f.setUpdatedAt(now);
        folderRepository.save(f);
    }

    private String normalizeKind(Object raw) {
        String kind = str(raw, KIND_TODOLIST);
        if (kind == null || !KINDS.contains(kind)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "kind must be todolist, companytracker, or document");
        }
        return kind;
    }

    private RoadmapFolderEntity load(UUID id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FOLDER_NOT_FOUND", "Folder not found"));
    }

    private Map<String, Object> toDto(RoadmapFolderEntity f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("folderId", f.getFolderId());
        m.put("name", f.getName());
        m.put("kind", f.getKind() == null ? KIND_TODOLIST : f.getKind());
        m.put("sortOrder", f.getSortOrder());
        long itemCount;
        if (KIND_COMPANYTRACKER.equals(f.getKind())) {
            itemCount = companyRepository.countByFolderId(f.getFolderId());
        } else if (KIND_DOCUMENT.equals(f.getKind())) {
            itemCount = documentRepository.countByFolderId(f.getFolderId());
        } else {
            itemCount = todoRepository.countByFolderId(f.getFolderId());
        }
        m.put("itemCount", itemCount);
        m.put("todoCount", itemCount); // back-compat for FE during transition
        m.put("updatedAt", f.getUpdatedAt());
        return m;
    }

    private String str(Object v, String fallback) {
        if (v == null) {
            return fallback;
        }
        String s = String.valueOf(v);
        return s.isBlank() ? fallback : s;
    }
}
