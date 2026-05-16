package com.example.examauth.controller;

import com.example.examauth.dto.SystemSettingsDTO;
import com.example.examauth.dto.SystemSettingsUpdateRequestDTO;
import com.example.examauth.model.SupervisorActionLog;
import com.example.examauth.repo.SupervisorActionLogRepository;
import com.example.examauth.repo.SystemSettingRepository;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.service.SettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class SystemSettingsController {

    private final SettingsService settingsService;
    private final com.example.examauth.util.JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final SupervisorActionLogRepository supervisorActionLogRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final org.springframework.cache.CacheManager cacheManager;

    public SystemSettingsController(
            SettingsService settingsService,
            com.example.examauth.util.JwtUtil jwtUtil,
            UserRepository userRepository,
            SystemSettingRepository systemSettingRepository,
            SupervisorActionLogRepository supervisorActionLogRepository,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            org.springframework.cache.CacheManager cacheManager) {
        this.settingsService = settingsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.supervisorActionLogRepository = supervisorActionLogRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.cacheManager = cacheManager;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> getAllSettings() {
        return ResponseEntity.ok(settingsService.getAllSettings());
    }

    @GetMapping("/typed")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> getAllSettingsTyped() {
        return ResponseEntity.ok(SystemSettingsDTO.fromMap(settingsService.getAllSettings()));
    }

    @GetMapping("/meta")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> getSettingsMeta() {
        String fallbackEmail = settingsService.getSetting(SettingsService.KEY_ADMIN_CONTACT_EMAIL, "admin@examhub.com");
        String lastBackupAt = settingsService.getSetting(SettingsService.KEY_LAST_BACKUP_AT, "");
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("adminContactEmail", fallbackEmail);
        meta.put("lastBackupAt", lastBackupAt);
        systemSettingRepository.findTopByOrderByUpdatedAtDesc().ifPresentOrElse(latest -> {
            meta.put("updatedBy", latest.getUpdatedBy());
            meta.put("updatedAt", latest.getUpdatedAt());
        }, () -> {
            meta.put("updatedBy", "Super Admin");
            meta.put("updatedAt", null);
        });
        return ResponseEntity.ok(meta);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> updateSettings(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> settings) {
        try {
            String updatedBy = "system";
            Long actorId = null;
            if (token != null && token.startsWith("Bearer ")) {
                String email = jwtUtil.extractUsername(token.substring(7));
                updatedBy = email;
                actorId = userRepository.findFirstByEmail(email).map(u -> u.getUserId()).orElse(null);
            }

            settingsService.updateSettings(settings, updatedBy);

            SupervisorActionLog log = new SupervisorActionLog();
            log.setSupervisorId(actorId);
            log.setAction("SYSTEM_SETTINGS_UPDATED");
            log.setDetails("Updated " + (settings == null ? 0 : settings.size()) + " settings by " + updatedBy);
            log.setTimestamp(LocalDateTime.now());
            supervisorActionLogRepository.save(log);

            return ResponseEntity.ok(Map.of("success", true, "message", "System settings updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update settings: " + e.getMessage()));
        }
    }

    @PutMapping("/typed")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> updateSettingsTyped(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody SystemSettingsUpdateRequestDTO request) {
        Map<String, String> payload = new LinkedHashMap<>();
        if (request != null && request.getSettings() != null) {
            payload.putAll(request.getSettings().toMap());
        }
        if (request != null && request.getRawSettings() != null) {
            payload.putAll(request.getRawSettings());
        }
        return updateSettings(token, payload);
    }

    @PostMapping("/maintenance/backup")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> backupDatabase(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            String updatedBy = "system";
            if (token != null && token.startsWith("Bearer ")) {
                updatedBy = jwtUtil.extractUsername(token.substring(7));
            }

            List<String> tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("generatedAt", LocalDateTime.now().toString());
            snapshot.put("generatedBy", updatedBy);
            snapshot.put("backupSchedule", settingsService.getSetting(SettingsService.KEY_BACKUP_SCHEDULE, "Daily"));

            Map<String, Object> tableStats = new LinkedHashMap<>();
            Map<String, Object> tablesData = new LinkedHashMap<>();
            for (String table : tables) {
                try {
                    Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Long.class);
                    tableStats.put(table, Map.of("rowCount", count == null ? 0 : count));
                    List<Map<String, Object>> columns = jdbcTemplate.queryForList("SHOW COLUMNS FROM `" + table + "`");
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + table + "`");
                    Map<String, Object> tableSnapshot = new LinkedHashMap<>();
                    tableSnapshot.put("columns", columns);
                    tableSnapshot.put("rows", rows);
                    tablesData.put(table, tableSnapshot);
                } catch (Exception ex) {
                    tableStats.put(table, Map.of("error", "Failed to count rows: " + ex.getMessage()));
                    tablesData.put(table, Map.of("error", "Failed to export table data: " + ex.getMessage()));
                }
            }
            snapshot.put("tableStats", tableStats);
            snapshot.put("tables", tablesData);
            snapshot.put("settingsSnapshot", settingsService.getAllSettings());

            Path backupDir = Path.of("backups");
            Files.createDirectories(backupDir);
            String filename = "db_backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + ".json";
            Path backupFile = backupDir.resolve(filename);

            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(backupFile.toFile(), snapshot);

            settingsService.updateSettings(
                    Map.of(SettingsService.KEY_LAST_BACKUP_AT, LocalDateTime.now().toString()),
                    updatedBy);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Database backup created successfully",
                    "file", backupFile.toAbsolutePath().toString(),
                    "backupAt", LocalDateTime.now().toString()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Backup failed: " + e.getMessage()));
        }
    }

    @PostMapping("/maintenance/backup-schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> updateBackupSchedule(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> body) {
        try {
            String schedule = body == null ? null : body.get("schedule");
            if (schedule == null || schedule.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "schedule is required"));
            }

            String updatedBy = "system";
            if (token != null && token.startsWith("Bearer ")) {
                updatedBy = jwtUtil.extractUsername(token.substring(7));
            }
            settingsService.updateSettings(Map.of(SettingsService.KEY_BACKUP_SCHEDULE, schedule), updatedBy);
            return ResponseEntity.ok(Map.of("success", true, "message", "Backup schedule updated", "schedule", schedule));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update backup schedule: " + e.getMessage()));
        }
    }

    @GetMapping("/maintenance/logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> getMaintenanceLogs() {
        try {
            List<SupervisorActionLog> rows = supervisorActionLogRepository.findTop100ByOrderByTimestampDesc();
            List<Map<String, Object>> logs = new ArrayList<>();
            for (SupervisorActionLog row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("timestamp", row.getTimestamp());
                item.put("action", row.getAction());
                item.put("details", row.getDetails());
                item.put("actorId", row.getSupervisorId());
                logs.add(item);
            }
            return ResponseEntity.ok(Map.of("logs", logs));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load logs: " + e.getMessage()));
        }
    }

    @PostMapping("/maintenance/clear-cache")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> clearSystemCache() {
        try {
            Map<String, Object> before = getCacheStatusData();
            if (cacheManager != null && cacheManager.getCacheNames() != null) {
                for (String cacheName : cacheManager.getCacheNames()) {
                    org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                    }
                }
            }
            Map<String, Object> after = getCacheStatusData();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "System cache cleared successfully",
                    "before", before,
                    "after", after));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to clear cache: " + e.getMessage()));
        }
    }

    @GetMapping("/maintenance/cache-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> getCacheStatus() {
        try {
            return ResponseEntity.ok(getCacheStatusData());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to read cache status: " + e.getMessage()));
        }
    }

    private Map<String, Object> getCacheStatusData() {
        Map<String, Object> status = new LinkedHashMap<>();
        List<Map<String, Object>> caches = new ArrayList<>();
        int totalEntries = 0;
        long estimatedBytes = 0L;

        if (cacheManager != null && cacheManager.getCacheNames() != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                int entries = 0;

                Object nativeCache = cache != null ? cache.getNativeCache() : null;
                if (nativeCache instanceof java.util.Map<?, ?> map) {
                    entries = map.size();
                } else {
                    try {
                        java.lang.reflect.Method sizeMethod = nativeCache != null ? nativeCache.getClass().getMethod("size") : null;
                        if (sizeMethod != null) {
                            Object result = sizeMethod.invoke(nativeCache);
                            if (result instanceof Number n) {
                                entries = n.intValue();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                totalEntries += entries;
                estimatedBytes += (long) entries * 1024L; // approx 1KB per entry
                caches.add(Map.of("name", cacheName, "entries", entries));
            }
        }

        status.put("cacheCount", caches.size());
        status.put("totalEntries", totalEntries);
        status.put("estimatedBytes", estimatedBytes);
        status.put("estimatedHumanSize", humanReadableSize(estimatedBytes));
        status.put("caches", caches);
        return status;
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.2f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }
}
