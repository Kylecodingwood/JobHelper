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

import com.jobhelper.roadmap.infrastructure.RoadmapCompanyEntity;
import com.jobhelper.roadmap.infrastructure.RoadmapCompanyRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapFolderEntity;
import com.jobhelper.shared.web.ApiException;

@Service
public class RoadmapCompanyService {
    public static final Set<String> STATUSES = Set.of(
            "watching", "applied", "interview", "offer", "rejected", "on_hold");

    private final RoadmapCompanyRepository repository;
    private final RoadmapFolderService folderService;

    public RoadmapCompanyService(RoadmapCompanyRepository repository, RoadmapFolderService folderService) {
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
        String companyName = str(body == null ? null : body.get("companyName"), null);
        if (companyName == null || companyName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "companyName required");
        }
        UUID folderId = folderService.resolveFolderId(parseUuid(body == null ? null : body.get("folderId")));
        RoadmapFolderEntity folder = folderService.requireFolder(folderId);
        requireKind(folder, RoadmapFolderService.KIND_COMPANYTRACKER);

        Instant now = Instant.now();
        RoadmapCompanyEntity c = new RoadmapCompanyEntity();
        c.setCompanyId(UUID.randomUUID());
        c.setFolderId(folderId);
        c.setCompanyName(companyName.trim());
        c.setStatus(normalizeStatus(body == null ? null : body.get("status"), "watching"));
        c.setContact(str(body == null ? null : body.get("contact"), null));
        c.setNote(str(body == null ? null : body.get("note"), null));
        c.setSortOrder((int) repository.countByFolderId(folderId));
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        repository.save(c);
        return toDto(c);
    }

    @Transactional
    public Map<String, Object> patch(UUID companyId, Map<String, Object> body) {
        RoadmapCompanyEntity c = load(companyId);
        if (body != null) {
            if (body.containsKey("companyName")) {
                String name = str(body.get("companyName"), null);
                if (name == null || name.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "companyName required");
                }
                c.setCompanyName(name.trim());
            }
            if (body.containsKey("status")) {
                c.setStatus(normalizeStatus(body.get("status"), c.getStatus()));
            }
            if (body.containsKey("contact")) {
                c.setContact(str(body.get("contact"), null));
            }
            if (body.containsKey("note")) {
                c.setNote(str(body.get("note"), null));
            }
            if (body.containsKey("folderId")) {
                UUID folderId = parseUuid(body.get("folderId"));
                if (folderId == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "folderId required");
                }
                RoadmapFolderEntity folder = folderService.requireFolder(folderId);
                requireKind(folder, RoadmapFolderService.KIND_COMPANYTRACKER);
                c.setFolderId(folderId);
            }
        }
        c.setUpdatedAt(Instant.now());
        repository.save(c);
        return toDto(c);
    }

    @Transactional
    public void delete(UUID companyId) {
        if (!repository.existsById(companyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company not found");
        }
        repository.deleteById(companyId);
    }

    private void requireKind(RoadmapFolderEntity folder, String kind) {
        if (!kind.equals(folder.getKind())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FOLDER_KIND",
                    "Folder kind must be " + kind);
        }
    }

    private String normalizeStatus(Object raw, String fallback) {
        String s = str(raw, fallback);
        if (s == null || !STATUSES.contains(s)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "status must be one of " + STATUSES);
        }
        return s;
    }

    private RoadmapCompanyEntity load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "Company not found"));
    }

    private Map<String, Object> toDto(RoadmapCompanyEntity c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("companyId", c.getCompanyId());
        m.put("folderId", c.getFolderId());
        m.put("companyName", c.getCompanyName());
        m.put("status", c.getStatus());
        m.put("contact", c.getContact());
        m.put("note", c.getNote());
        m.put("sortOrder", c.getSortOrder());
        m.put("updatedAt", c.getUpdatedAt());
        return m;
    }

    private String str(Object v, String fallback) {
        if (v == null) {
            return fallback;
        }
        String s = String.valueOf(v);
        return s.isBlank() || "null".equals(s) ? fallback : s;
    }

    private UUID parseUuid(Object v) {
        if (v == null || String.valueOf(v).isBlank() || "null".equals(String.valueOf(v))) {
            return null;
        }
        return UUID.fromString(String.valueOf(v));
    }
}
