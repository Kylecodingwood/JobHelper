package com.jobhelper.roadmap.api;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jobhelper.roadmap.application.RoadmapCompanyService;
import com.jobhelper.roadmap.application.RoadmapFolderService;
import com.jobhelper.roadmap.application.RoadmapTodoService;

@RestController
@RequestMapping("/api/v1/roadmap")
public class RoadmapController {
    private final RoadmapTodoService todoService;
    private final RoadmapFolderService folderService;
    private final RoadmapCompanyService companyService;

    public RoadmapController(
            RoadmapTodoService todoService,
            RoadmapFolderService folderService,
            RoadmapCompanyService companyService) {
        this.todoService = todoService;
        this.folderService = folderService;
        this.companyService = companyService;
    }

    /** Folders + todos or companies for one folder. */
    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) UUID folderId) {
        return todoService.list(folderId);
    }

    @GetMapping("/folders")
    public Map<String, Object> folders() {
        return Map.of("folders", folderService.listFolders());
    }

    @PostMapping("/folders")
    public ResponseEntity<Map<String, Object>> createFolder(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(folderService.create(body));
    }

    @PatchMapping("/folders/{folderId}")
    public Map<String, Object> patchFolder(@PathVariable UUID folderId, @RequestBody Map<String, Object> body) {
        return folderService.patch(folderId, body);
    }

    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable UUID folderId) {
        folderService.delete(folderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/todos")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.create(body));
    }

    @PatchMapping("/todos/{todoId}")
    public Map<String, Object> patch(@PathVariable UUID todoId, @RequestBody Map<String, Object> body) {
        return todoService.patch(todoId, body);
    }

    @PostMapping("/todos/{todoId}/toggle")
    public Map<String, Object> toggle(@PathVariable UUID todoId) {
        return todoService.toggle(todoId);
    }

    @DeleteMapping("/todos/{todoId}")
    public ResponseEntity<Void> delete(@PathVariable UUID todoId) {
        todoService.delete(todoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/companies")
    public ResponseEntity<Map<String, Object>> createCompany(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(body));
    }

    @PatchMapping("/companies/{companyId}")
    public Map<String, Object> patchCompany(@PathVariable UUID companyId, @RequestBody Map<String, Object> body) {
        return companyService.patch(companyId, body);
    }

    @DeleteMapping("/companies/{companyId}")
    public ResponseEntity<Void> deleteCompany(@PathVariable UUID companyId) {
        companyService.delete(companyId);
        return ResponseEntity.noContent().build();
    }
}
