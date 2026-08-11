package com.jobhelper.profile.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.job.infrastructure.CanonicalJobRepository;
import com.jobhelper.profile.infrastructure.ProfileBackupMetadataEntity;
import com.jobhelper.profile.infrastructure.ProfileBackupMetadataRepository;
import com.jobhelper.profile.infrastructure.ProfileEntity;
import com.jobhelper.profile.infrastructure.ProfileRepository;
import com.jobhelper.roadmap.infrastructure.PreviewSessionEntity;
import com.jobhelper.roadmap.infrastructure.PreviewSessionRepository;
import com.jobhelper.roadmap.infrastructure.RoadmapRepository;
import com.jobhelper.shared.web.ApiException;

/**
 * Full-database backup/export and preview-then-confirm restore, per
 * delta/2.design/server/profile/profile-workflow-backup/delta.md.
 *
 * Backup package layout (zip): manifest.json, database.dump (pg_dump -Fc) or
 * database.json (fallback when pg_dump is unavailable at export time), files/.
 */
@Service
public class BackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final int RETAIN = 4;
    private static final String APP_SCHEMA_VERSION = "1.0.0";
    private static final List<String> DOMAINS_INCLUDED = List.of("profile", "roadmap", "job", "action");
    private static final String RESTORE_PREVIEW_KIND = "PROFILE_RESTORE";
    private static final Duration RESTORE_TOKEN_TTL = Duration.ofMinutes(15);
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
            .withZone(ZoneOffset.UTC);
    private static final Pattern JDBC_URL_PATTERN =
            Pattern.compile("^jdbc:postgresql://(?<host>[^:/]+)(?::(?<port>\\d+))?/(?<db>[^?;]+).*$");

    private final ProfileBackupMetadataRepository backupRepository;
    private final ProfileRepository profileRepository;
    private final RoadmapRepository roadmapRepository;
    private final CanonicalJobRepository jobRepository;
    private final PreviewSessionRepository previewSessionRepository;
    private final ObjectMapper objectMapper;

    @Value("${jobhelper.backup.dir:./data/backups}")
    private String backupDir;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:jobhelper}")
    private String datasourceUser;

    @Value("${spring.datasource.password:jobhelper}")
    private String datasourcePassword;

    public BackupService(
            ProfileBackupMetadataRepository backupRepository,
            ProfileRepository profileRepository,
            RoadmapRepository roadmapRepository,
            CanonicalJobRepository jobRepository,
            PreviewSessionRepository previewSessionRepository,
            ObjectMapper objectMapper) {
        this.backupRepository = backupRepository;
        this.profileRepository = profileRepository;
        this.roadmapRepository = roadmapRepository;
        this.jobRepository = jobRepository;
        this.previewSessionRepository = previewSessionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list() {
        List<Map<String, Object>> items = backupRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toItem)
                .toList();
        return Map.of(
                "items", items,
                "retentionPolicy", Map.of("maxScheduledCopies", RETAIN, "scheduleCron", "0 0 3 * * SUN"));
    }

    @Transactional
    public Map<String, Object> export(String targetPath, String backupType) {
        try {
            Path dir = Path.of(backupDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            UUID backupId = UUID.randomUUID();
            String type = backupType == null || backupType.isBlank() ? "MANUAL_EXPORT" : backupType;

            Path file;
            if (targetPath != null && !targetPath.isBlank()) {
                file = Path.of(targetPath).toAbsolutePath().normalize();
            } else {
                file = dir.resolve("job-helper-backup-" + FILE_TIMESTAMP.format(Instant.now()) + ".zip");
            }
            Files.createDirectories(file.getParent());

            int profileVersion = profileRepository.findSingleton().map(ProfileEntity::getProfileVersion).orElse(0);
            byte[] fileChecksum = buildBackupZip(file, type, profileVersion);
            String checksum = HexFormat.of().formatHex(fileChecksum);

            ProfileBackupMetadataEntity meta = new ProfileBackupMetadataEntity();
            meta.setBackupId(backupId);
            meta.setBackupType(type);
            meta.setFilePath(file.toString());
            meta.setChecksum(checksum);
            meta.setDomainsIncluded(String.join(",", DOMAINS_INCLUDED));
            meta.setRetentionRank(null);
            meta.setCreatedAt(Instant.now());
            backupRepository.save(meta);
            if ("SCHEDULED".equals(type)) {
                enforceRetention();
            }
            return toItem(meta);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "BACKUP_FAILED", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> restorePreview(UUID backupId) {
        ProfileBackupMetadataEntity meta = load(backupId);
        Path file = Path.of(meta.getFilePath());
        if (!Files.exists(file)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BACKUP_FILE_MISSING", "Backup file not found on disk");
        }

        byte[] bytes;
        String actualChecksum;
        try {
            bytes = Files.readAllBytes(file);
            actualChecksum = sha256(bytes);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "BACKUP_FILE_UNREADABLE", e.getMessage());
        }
        if (meta.getChecksum() != null && !meta.getChecksum().isBlank() && !meta.getChecksum().equals(actualChecksum)) {
            throw new ApiException(HttpStatus.CONFLICT, "BACKUP_INCOMPATIBLE",
                    "Backup file checksum mismatch; file may be corrupted or modified since export");
        }

        Map<String, Object> manifest = readManifest(file);
        String manifestSchemaVersion = manifest == null ? null : str(manifest.get("appSchemaVersion"), null);
        if (manifest != null && manifestSchemaVersion != null && !APP_SCHEMA_VERSION.equals(manifestSchemaVersion)) {
            throw new ApiException(HttpStatus.CONFLICT, "BACKUP_INCOMPATIBLE",
                    "Backup schema version " + manifestSchemaVersion + " is incompatible with current " + APP_SCHEMA_VERSION);
        }

        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(RESTORE_TOKEN_TTL);
        Map<String, Object> sessionPayload = new LinkedHashMap<>();
        sessionPayload.put("backupId", backupId.toString());
        sessionPayload.put("filePath", file.toString());
        sessionPayload.put("checksumSha256", actualChecksum);
        savePreviewSession(token, sessionPayload, now, expiresAt);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("backupId", meta.getBackupId());
        m.put("restorePreviewToken", token);
        m.put("expiresAt", expiresAt.toString());
        m.put("filePath", meta.getFilePath());
        m.put("domainsIncluded", meta.getDomainsIncluded());
        m.put("appSchemaVersion", manifestSchemaVersion);
        m.put("profileVersion", manifest == null ? null : manifest.get("profileVersion"));
        m.put("format", manifest == null ? "LEGACY" : manifest.get("format"));
        m.put("warning", "Restore will overwrite the entire current database with this backup's contents; "
                + "this cannot be automatically undone");
        m.put("recommendedAction", "Recompute Gate/Roadmap after restore is recommended but will NOT run automatically");
        m.put("confirmRequired", true);
        return m;
    }

    @Transactional
    public Map<String, Object> restore(UUID backupId, boolean confirmOverwrite, String restorePreviewToken) {
        if (!confirmOverwrite) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "confirmOverwrite must be true");
        }
        if (restorePreviewToken == null || restorePreviewToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "restorePreviewToken is required");
        }
        PreviewSessionEntity session = previewSessionRepository.findById(restorePreviewToken)
                .filter(s -> RESTORE_PREVIEW_KIND.equals(s.getKind()))
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED",
                        "Restore preview token not found or expired"));
        if (Instant.now().isAfter(session.getExpiresAt())) {
            previewSessionRepository.deleteById(restorePreviewToken);
            throw new ApiException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED", "Restore preview token expired");
        }
        Map<String, Object> sessionPayload = readJson(session.getPayload());
        if (!backupId.toString().equals(String.valueOf(sessionPayload.get("backupId")))) {
            throw new ApiException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED",
                    "Restore preview token does not match backupId");
        }

        ProfileBackupMetadataEntity meta = load(backupId);
        Path file = Path.of(meta.getFilePath());
        if (!Files.exists(file)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BACKUP_FILE_MISSING", "Backup file not found on disk");
        }

        String message;
        try {
            RestorePayload payload = extractRestorePayload(file);
            if (payload.dumpBytes() != null) {
                runPgRestore(payload.dumpBytes());
                message = "Database restored from pg_dump snapshot via pg_restore --clean --if-exists";
            } else if (payload.jsonBytes() != null) {
                restoreFromJson(payload.jsonBytes());
                message = "Legacy JSON backup restored via best-effort entity fallback (no pg_dump payload present)";
            } else {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "BACKUP_INCOMPATIBLE",
                        "Backup contains neither database.dump nor a JSON fallback payload");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RESTORE_FAILED", e.getMessage());
        }

        previewSessionRepository.deleteById(restorePreviewToken);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "COMPLETED");
        result.put("backupId", backupId);
        result.put("message", message);
        return result;
    }

    @Scheduled(cron = "${jobhelper.backup.cron:0 0 3 * * SUN}")
    @Transactional
    public void scheduledBackup() {
        try {
            export(null, "SCHEDULED");
        } catch (Exception e) {
            log.warn("Scheduled backup failed: {}", e.getMessage());
        }
    }

    // ---- Backup package construction ---------------------------------------

    /** Writes manifest.json + database.dump|database.json + files/ into a zip; returns the zip's SHA-256. */
    private byte[] buildBackupZip(Path file, String backupType, int profileVersion) throws Exception {
        Path dumpTemp = null;
        byte[] jsonFallback = null;
        String contentChecksum;
        boolean usedPgDump = false;
        try {
            if (canPgDump()) {
                dumpTemp = Files.createTempFile("jobhelper-backup-", ".dump");
                try {
                    runPgDump(dumpTemp);
                    usedPgDump = true;
                } catch (Exception e) {
                    log.warn("pg_dump failed, falling back to JSON export: {}", e.getMessage());
                    Files.deleteIfExists(dumpTemp);
                    dumpTemp = null;
                }
            }
            if (usedPgDump) {
                contentChecksum = sha256(Files.readAllBytes(dumpTemp));
            } else {
                jsonFallback = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(buildJsonPayload());
                contentChecksum = sha256(jsonFallback);
            }

            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("appSchemaVersion", APP_SCHEMA_VERSION);
            manifest.put("backupType", backupType);
            manifest.put("domainsIncluded", DOMAINS_INCLUDED);
            manifest.put("profileVersion", profileVersion);
            manifest.put("createdAt", Instant.now().toString());
            manifest.put("format", usedPgDump ? "PG_DUMP_CUSTOM" : "JSON_FALLBACK");
            manifest.put("checksumSha256", contentChecksum);
            byte[] manifestBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);

            try (OutputStream os = Files.newOutputStream(file);
                    ZipOutputStream zos = new ZipOutputStream(os)) {
                zos.putNextEntry(new ZipEntry("manifest.json"));
                zos.write(manifestBytes);
                zos.closeEntry();

                if (usedPgDump) {
                    zos.putNextEntry(new ZipEntry("database.dump"));
                    Files.copy(dumpTemp, zos);
                    zos.closeEntry();
                } else {
                    zos.putNextEntry(new ZipEntry("database.json"));
                    zos.write(jsonFallback);
                    zos.closeEntry();
                }

                zos.putNextEntry(new ZipEntry("files/"));
                zos.closeEntry();
            }
        } finally {
            if (dumpTemp != null) {
                Files.deleteIfExists(dumpTemp);
            }
        }
        return sha256Bytes(Files.readAllBytes(file));
    }

    private Map<String, Object> buildJsonPayload() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("exportedAt", Instant.now().toString());
        profileRepository.findSingleton().ifPresent(p -> root.put("profile", Map.of(
                "profileId", p.getProfileId(),
                "profileVersion", p.getProfileVersion(),
                "lifecycle", p.getLifecycle())));
        roadmapRepository.findFirstByStatusOrderByUpdatedAtDesc("ACTIVE").ifPresent(r -> root.put("roadmap", Map.of(
                "roadmapId", r.getRoadmapId(),
                "version", r.getVersion(),
                "taskCount", r.getTasks().size())));
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (CanonicalJobEntity j : jobRepository.findAll()) {
            jobs.add(Map.of(
                    "jobId", j.getJobId(),
                    "title", j.getTitle() == null ? "" : j.getTitle(),
                    "jobStatus", j.getJobStatus(),
                    "gateStatus", j.getGateStatus()));
        }
        root.put("jobs", jobs);
        return root;
    }

    // ---- Restore payload extraction / application --------------------------

    private record RestorePayload(byte[] dumpBytes, byte[] jsonBytes) {}

    private RestorePayload extractRestorePayload(Path file) throws IOException {
        if (isZip(file)) {
            try (ZipFile zip = new ZipFile(file.toFile())) {
                ZipEntry dumpEntry = zip.getEntry("database.dump");
                if (dumpEntry != null) {
                    return new RestorePayload(readAllBytes(zip.getInputStream(dumpEntry)), null);
                }
                ZipEntry jsonEntry = zip.getEntry("database.json");
                if (jsonEntry != null) {
                    return new RestorePayload(null, readAllBytes(zip.getInputStream(jsonEntry)));
                }
                return new RestorePayload(null, null);
            }
        }
        byte[] raw = Files.readAllBytes(file);
        if (file.getFileName().toString().endsWith(".json")) {
            return new RestorePayload(null, raw);
        }
        return new RestorePayload(raw, null);
    }

    @SuppressWarnings("unchecked")
    private void restoreFromJson(byte[] jsonBytes) {
        Map<String, Object> root = readJson(new String(jsonBytes, StandardCharsets.UTF_8));
        Object profileObj = root.get("profile");
        if (profileObj instanceof Map<?, ?> pm) {
            profileRepository.findSingleton().ifPresent(profile -> {
                Object lifecycle = pm.get("lifecycle");
                if (lifecycle != null) {
                    profile.setLifecycle(String.valueOf(lifecycle));
                }
                Object version = pm.get("profileVersion");
                if (version != null) {
                    try {
                        profile.setProfileVersion(Integer.parseInt(String.valueOf(version)));
                    } catch (NumberFormatException ignored) {
                        // keep current version if payload malformed
                    }
                }
                profileRepository.save(profile);
            });
        }
        Object jobsObj = root.get("jobs");
        if (jobsObj instanceof List<?> jobs) {
            for (Object o : jobs) {
                if (!(o instanceof Map<?, ?> jm) || jm.get("jobId") == null) {
                    continue;
                }
                try {
                    UUID jobId = UUID.fromString(String.valueOf(jm.get("jobId")));
                    jobRepository.findById(jobId).ifPresent(job -> {
                        Object status = jm.get("jobStatus");
                        if (status != null) {
                            job.setJobStatus(String.valueOf(status));
                        }
                        Object gate = jm.get("gateStatus");
                        if (gate != null) {
                            job.setGateStatus(String.valueOf(gate));
                        }
                        jobRepository.save(job);
                    });
                } catch (Exception ignored) {
                    // skip malformed job entries
                }
            }
        }
    }

    // ---- pg_dump / pg_restore -----------------------------------------------

    private boolean canPgDump() {
        return toolAvailable("pg_dump");
    }

    private boolean canPgRestore() {
        return toolAvailable("pg_restore");
    }

    private boolean toolAvailable(String executable) {
        try {
            Process p = new ProcessBuilder(executable, "--version").redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void runPgDump(Path file) throws Exception {
        JdbcTarget target = parseJdbcUrl(datasourceUrl);
        List<String> cmd = List.of("pg_dump", "-Fc",
                "-h", target.host(), "-p", String.valueOf(target.port()),
                "-U", datasourceUser, "-f", file.toString(), target.database());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("PGPASSWORD", datasourcePassword);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = readAllText(p.getInputStream());
        int code = p.waitFor();
        if (code != 0) {
            throw new IllegalStateException("pg_dump failed: " + out);
        }
    }

    private void runPgRestore(byte[] dumpBytes) throws Exception {
        if (!canPgRestore()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "RESTORE_TOOLING_UNAVAILABLE",
                    "pg_restore is not available on this host; restore database.dump manually via ops tooling");
        }
        JdbcTarget target = parseJdbcUrl(datasourceUrl);
        Path tempDump = Files.createTempFile("jobhelper-restore-", ".dump");
        try {
            Files.write(tempDump, dumpBytes);
            List<String> cmd = List.of("pg_restore", "--clean", "--if-exists", "--no-owner",
                    "-h", target.host(), "-p", String.valueOf(target.port()),
                    "-U", datasourceUser, "-d", target.database(), tempDump.toString());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("PGPASSWORD", datasourcePassword);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = readAllText(p.getInputStream());
            int code = p.waitFor();
            if (code != 0) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RESTORE_FAILED", "pg_restore failed: " + out);
            }
        } finally {
            Files.deleteIfExists(tempDump);
        }
    }

    private record JdbcTarget(String host, int port, String database) {}

    private JdbcTarget parseJdbcUrl(String url) {
        String host = "localhost";
        int port = 5432;
        String db = "jobhelper";
        if (url != null && !url.isBlank()) {
            Matcher m = JDBC_URL_PATTERN.matcher(url.trim());
            if (m.matches()) {
                host = m.group("host");
                String portGroup = m.group("port");
                if (portGroup != null && !portGroup.isBlank()) {
                    port = Integer.parseInt(portGroup);
                }
                db = m.group("db");
            }
        }
        return new JdbcTarget(host, port, db);
    }

    // ---- Retention ------------------------------------------------------------

    private void enforceRetention() {
        List<ProfileBackupMetadataEntity> scheduled =
                new ArrayList<>(backupRepository.findByBackupTypeOrderByCreatedAtDesc("SCHEDULED"));
        for (int i = 0; i < scheduled.size(); i++) {
            ProfileBackupMetadataEntity m = scheduled.get(i);
            m.setRetentionRank(i + 1);
            backupRepository.save(m);
        }
        for (int i = RETAIN; i < scheduled.size(); i++) {
            ProfileBackupMetadataEntity old = scheduled.get(i);
            try {
                Files.deleteIfExists(Path.of(old.getFilePath()));
            } catch (Exception ignored) {
                // best-effort
            }
            backupRepository.delete(old);
        }
    }

    // ---- Preview session (durable, shared with roadmap preview flows) ------

    private void savePreviewSession(String token, Map<String, Object> payload, Instant now, Instant expiresAt) {
        PreviewSessionEntity session = new PreviewSessionEntity();
        session.setPreviewToken(token);
        session.setKind(RESTORE_PREVIEW_KIND);
        session.setPayload(writeJson(payload));
        session.setCreatedAt(now);
        session.setExpiresAt(expiresAt);
        previewSessionRepository.save(session);
    }

    // ---- Zip / manifest helpers ---------------------------------------------

    private boolean isZip(Path file) {
        try (ZipFile ignored = new ZipFile(file.toFile())) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Map<String, Object> readManifest(Path file) {
        if (!isZip(file)) {
            return null;
        }
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry entry = zip.getEntry("manifest.json");
            if (entry == null) {
                return null;
            }
            byte[] bytes = readAllBytes(zip.getInputStream(entry));
            return readJson(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        try (InputStream is = in) {
            return is.readAllBytes();
        }
    }

    private String readAllText(InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return br.lines().reduce("", (a, b) -> a + b + "\n");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            Object v = objectMapper.readValue(json, Map.class);
            return v instanceof Map ? (Map<String, Object>) v : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "null";
        }
    }

    private String str(Object v, String fallback) {
        if (v == null) {
            return fallback;
        }
        String s = String.valueOf(v);
        return s.isBlank() ? fallback : s;
    }

    // ---- Metadata helpers -----------------------------------------------------

    private ProfileBackupMetadataEntity load(UUID id) {
        return backupRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BACKUP_NOT_FOUND", "Backup not found"));
    }

    private Map<String, Object> toItem(ProfileBackupMetadataEntity m) {
        Map<String, Object> item = new HashMap<>();
        item.put("backupId", m.getBackupId());
        item.put("backupType", m.getBackupType());
        item.put("filePath", m.getFilePath());
        item.put("checksum", m.getChecksum());
        item.put("domainsIncluded", m.getDomainsIncluded());
        item.put("retentionRank", m.getRetentionRank());
        item.put("createdAt", m.getCreatedAt());
        return item;
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(sha256Bytes(bytes));
    }

    private byte[] sha256Bytes(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(bytes);
    }
}
