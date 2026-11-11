package com.jobhelper.application.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobhelper.application.application.EmailApplicationService;

@RestController
@RequestMapping("/api/v1/applications")
public class EmailApplicationController {

    private final EmailApplicationService service;

    public EmailApplicationController(EmailApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return service.stats();
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> bulkImport(@RequestBody List<Map<String, Object>> items) {
        return ResponseEntity.ok(service.bulkImport(items));
    }

    @PatchMapping("/{appId}")
    public Map<String, Object> patch(@PathVariable UUID appId, @RequestBody Map<String, Object> body) {
        return service.patch(appId, body);
    }
}
