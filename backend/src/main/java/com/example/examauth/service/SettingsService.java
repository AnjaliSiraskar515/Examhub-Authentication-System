package com.example.examauth.service;

import com.example.examauth.model.SystemSetting;
import com.example.examauth.repo.SystemSettingRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class SettingsService {

    public static final String KEY_SYSTEM_NAME = "system.name";
    public static final String KEY_ADMIN_CONTACT_EMAIL = "admin.contact.email";
    public static final String KEY_SESSION_TIMEOUT_MINUTES = "session.timeout.minutes";
    public static final String KEY_DEFAULT_TIMEZONE = "default.timezone";
    public static final String KEY_MAX_STUDENTS_PER_EXAM = "exam.max.students";
    public static final String KEY_ENABLE_SYSTEM_LOGS = "system.logs.enabled";
    public static final String KEY_ALERT_THRESHOLD = "notifications.alert.threshold";
    public static final String KEY_EMAIL_DIGEST_FREQUENCY = "notifications.email.digest.frequency";
    public static final String KEY_SMS_ALERTS_ENABLED = "notifications.sms.alerts.enabled";
    public static final String KEY_AUTO_REPORT_INCIDENTS = "notifications.auto.report.incidents";
    public static final String KEY_REQUIRE_INSTITUTION_APPROVAL = "institution.approval.required";
    public static final String KEY_AUTO_SUSPEND_INACTIVE = "institution.auto.suspend.inactive";
    public static final String KEY_MAX_ADMINS_PER_INSTITUTION = "institution.max.admins";
    public static final String KEY_BACKUP_SCHEDULE = "maintenance.backup.schedule";
    public static final String KEY_LAST_BACKUP_AT = "maintenance.last.backup.at";
    private static final Set<String> ALLOWED_KEYS = new HashSet<>(Arrays.asList(
            KEY_SYSTEM_NAME,
            KEY_ADMIN_CONTACT_EMAIL,
            KEY_SESSION_TIMEOUT_MINUTES,
            KEY_DEFAULT_TIMEZONE,
            KEY_MAX_STUDENTS_PER_EXAM,
            KEY_ENABLE_SYSTEM_LOGS,
            KEY_ALERT_THRESHOLD,
            KEY_EMAIL_DIGEST_FREQUENCY,
            KEY_SMS_ALERTS_ENABLED,
            KEY_AUTO_REPORT_INCIDENTS,
            KEY_REQUIRE_INSTITUTION_APPROVAL,
            KEY_AUTO_SUSPEND_INACTIVE,
            KEY_MAX_ADMINS_PER_INSTITUTION,
            KEY_BACKUP_SCHEDULE,
            KEY_LAST_BACKUP_AT));
    private static final Set<String> ALLOWED_TIMEZONES = new HashSet<>(Arrays.asList(
            "UTC", "IST (Indian Standard Time)", "EST (Eastern Standard Time)"));
    private static final Set<String> ALLOWED_ALERT_THRESHOLDS = new HashSet<>(Arrays.asList(
            "High Priority Only", "Medium & High", "All Alerts"));
    private static final Set<String> ALLOWED_DIGEST_FREQUENCIES = new HashSet<>(Arrays.asList(
            "Real-time", "Hourly", "Daily Summary"));

    private final SystemSettingRepository systemSettingRepository;

    public SettingsService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @Cacheable("systemSettingsAll")
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        Map<String, String> settings = new LinkedHashMap<>(getDefaultSettings());
        List<SystemSetting> rows = systemSettingRepository.findAll();
        for (SystemSetting row : rows) {
            settings.put(row.getKey(), row.getValue());
        }
        return settings;
    }

    @CacheEvict(value = { "systemSettingsAll", "systemSettingsByKey" }, allEntries = true)
    @Transactional
    public void updateSettings(Map<String, String> settings, String updatedBy) {
        if (settings == null || settings.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            String key = entry.getKey().trim();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            validateSetting(key, value);

            SystemSetting setting = systemSettingRepository.findByKey(key).orElseGet(SystemSetting::new);
            setting.setKey(key);
            setting.setValue(normalizeValue(key, value));
            setting.setUpdatedBy(updatedBy);
            setting.setUpdatedAt(now);
            systemSettingRepository.save(setting);
        }
    }

    @Cacheable(value = "systemSettingsByKey", key = "#key + '::' + #defaultValue")
    @Transactional(readOnly = true)
    public String getSetting(String key, String defaultValue) {
        Optional<SystemSetting> setting = systemSettingRepository.findByKey(key);
        return setting.map(SystemSetting::getValue).filter(v -> !v.isBlank()).orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public int getIntSetting(String key, int defaultValue) {
        String value = getSetting(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    @Transactional(readOnly = true)
    public boolean getBooleanSetting(String key, boolean defaultValue) {
        String value = getSetting(key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private void validateSetting(String key, String value) {
        if (!ALLOWED_KEYS.contains(key)) {
            throw new IllegalArgumentException("Unknown setting key: " + key);
        }
        switch (key) {
            case KEY_SYSTEM_NAME -> {
                if (value.isBlank() || value.length() > 120) {
                    throw new IllegalArgumentException("system.name must be 1-120 characters");
                }
            }
            case KEY_ADMIN_CONTACT_EMAIL -> {
                if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    throw new IllegalArgumentException("admin.contact.email must be a valid email");
                }
            }
            case KEY_SESSION_TIMEOUT_MINUTES -> {
                int minutes = parseInt(value, -1);
                if (minutes < 5 || minutes > 1440) {
                    throw new IllegalArgumentException("session.timeout.minutes must be between 5 and 1440");
                }
            }
            case KEY_MAX_STUDENTS_PER_EXAM -> {
                int max = parseInt(value, -1);
                if (max < 1 || max > 100000) {
                    throw new IllegalArgumentException("exam.max.students must be between 1 and 100000");
                }
            }
            case KEY_MAX_ADMINS_PER_INSTITUTION -> {
                int max = parseInt(value, -1);
                if (max < 1 || max > 1000) {
                    throw new IllegalArgumentException("institution.max.admins must be between 1 and 1000");
                }
            }
            case KEY_BACKUP_SCHEDULE -> {
                Set<String> allowed = Set.of("Hourly", "Daily", "Weekly", "Monthly");
                if (!allowed.contains(value)) {
                    throw new IllegalArgumentException(
                            "maintenance.backup.schedule must be Hourly/Daily/Weekly/Monthly");
                }
            }
            case KEY_LAST_BACKUP_AT -> {
                // Internal timestamp written by backup endpoint.
            }
            case KEY_DEFAULT_TIMEZONE -> {
                if (!ALLOWED_TIMEZONES.contains(value)) {
                    throw new IllegalArgumentException("default.timezone has unsupported value");
                }
            }
            case KEY_ALERT_THRESHOLD -> {
                if (!ALLOWED_ALERT_THRESHOLDS.contains(value)) {
                    throw new IllegalArgumentException("notifications.alert.threshold has unsupported value");
                }
            }
            case KEY_EMAIL_DIGEST_FREQUENCY -> {
                if (!ALLOWED_DIGEST_FREQUENCIES.contains(value)) {
                    throw new IllegalArgumentException("notifications.email.digest.frequency has unsupported value");
                }
            }
            case KEY_ENABLE_SYSTEM_LOGS, KEY_SMS_ALERTS_ENABLED, KEY_AUTO_REPORT_INCIDENTS,
                    KEY_REQUIRE_INSTITUTION_APPROVAL, KEY_AUTO_SUSPEND_INACTIVE -> {
                if (!isBooleanLike(value)) {
                    throw new IllegalArgumentException(key + " must be true/false");
                }
            }
            default -> {
                // No-op for forward compatibility.
            }
        }
    }

    private String normalizeValue(String key, String value) {
        if (key.equals(KEY_ENABLE_SYSTEM_LOGS)
                || key.equals(KEY_SMS_ALERTS_ENABLED)
                || key.equals(KEY_AUTO_REPORT_INCIDENTS)
                || key.equals(KEY_REQUIRE_INSTITUTION_APPROVAL)
                || key.equals(KEY_AUTO_SUSPEND_INACTIVE)) {
            return String.valueOf("true".equalsIgnoreCase(value) || "1".equals(value));
        }
        return value;
    }

    private boolean isBooleanLike(String value) {
        return "true".equalsIgnoreCase(value)
                || "false".equalsIgnoreCase(value)
                || "1".equals(value)
                || "0".equals(value);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Map<String, String> getDefaultSettings() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put(KEY_SYSTEM_NAME, "ExamHub Authentication System");
        defaults.put(KEY_ADMIN_CONTACT_EMAIL, "admin@examhub.com");
        defaults.put(KEY_SESSION_TIMEOUT_MINUTES, "480");
        defaults.put(KEY_DEFAULT_TIMEZONE, "IST (Indian Standard Time)");
        defaults.put(KEY_MAX_STUDENTS_PER_EXAM, "500");
        defaults.put(KEY_ENABLE_SYSTEM_LOGS, "true");
        defaults.put(KEY_ALERT_THRESHOLD, "High Priority Only");
        defaults.put(KEY_EMAIL_DIGEST_FREQUENCY, "Real-time");
        defaults.put(KEY_SMS_ALERTS_ENABLED, "true");
        defaults.put(KEY_AUTO_REPORT_INCIDENTS, "true");
        defaults.put(KEY_REQUIRE_INSTITUTION_APPROVAL, "true");
        defaults.put(KEY_AUTO_SUSPEND_INACTIVE, "false");
        defaults.put(KEY_MAX_ADMINS_PER_INSTITUTION, "5");
        defaults.put(KEY_BACKUP_SCHEDULE, "Daily");
        defaults.put(KEY_LAST_BACKUP_AT, "");
        return defaults;
    }
}
