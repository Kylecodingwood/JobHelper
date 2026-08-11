package com.jobhelper.profile.api;

import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobhelper.profile.api.ProfileDtos.ProfileAggregateDto;
import com.jobhelper.profile.api.ProfileDtos.ProfileWriteRequest;
import com.jobhelper.profile.api.ProfileDtos.ProfileWriteResponse;
import com.jobhelper.profile.application.BackupService;
import com.jobhelper.profile.application.ProfileService;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final BackupService backupService;

    public ProfileController(ProfileService profileService, BackupService backupService) {
        this.profileService = profileService;
        this.backupService = backupService;
    }

    @GetMapping
    public ProfileAggregateDto get() {
        return profileService.getProfile();
    }

    @PutMapping
    public ProfileWriteResponse put(@RequestBody ProfileWriteRequest request) {
        return profileService.save(request);
    }

    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody ProfileWriteRequest request) {
        return profileService.validate(request);
    }

    @GetMapping("/field-usage")
    public Map<String, Object> fieldUsage() {
        return profileService.fieldUsage();
    }

    @GetMapping("/backups")
    public Map<String, Object> backups() {
        return backupService.list();
    }

    public record ExportRequest(String targetPath) {}

    @PostMapping("/backups/export")
    public Map<String, Object> export(@RequestBody(required = false) ExportRequest body) {
        return backupService.export(body == null ? null : body.targetPath(), "MANUAL_EXPORT");
    }

    @GetMapping("/backups/{backupId}/restore-preview")
    public Map<String, Object> restorePreview(@PathVariable UUID backupId) {
        return backupService.restorePreview(backupId);
    }

    public record RestoreRequest(Boolean confirmOverwrite, String restorePreviewToken) {}

    @PostMapping("/backups/{backupId}/restore")
    public Map<String, Object> restore(@PathVariable UUID backupId, @RequestBody RestoreRequest body) {
        return backupService.restore(
                backupId,
                body != null && Boolean.TRUE.equals(body.confirmOverwrite()),
                body == null ? null : body.restorePreviewToken());
    }
}
