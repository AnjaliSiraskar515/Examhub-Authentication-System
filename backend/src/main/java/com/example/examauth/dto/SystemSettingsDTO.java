package com.example.examauth.dto;

import com.example.examauth.service.SettingsService;

import java.util.LinkedHashMap;
import java.util.Map;

public class SystemSettingsDTO {
    private String systemName;
    private String adminContactEmail;
    private String sessionTimeoutMinutes;
    private String defaultTimezone;
    private String maxStudentsPerExam;
    private String enableSystemLogs;
    private String alertThreshold;
    private String emailDigestFrequency;
    private String smsAlertsEnabled;
    private String autoReportIncidents;
    private String requireInstitutionApproval;
    private String autoSuspendInactive;
    private String maxAdminsPerInstitution;

    public static SystemSettingsDTO fromMap(Map<String, String> map) {
        SystemSettingsDTO dto = new SystemSettingsDTO();
        dto.setSystemName(map.get(SettingsService.KEY_SYSTEM_NAME));
        dto.setAdminContactEmail(map.get(SettingsService.KEY_ADMIN_CONTACT_EMAIL));
        dto.setSessionTimeoutMinutes(map.get(SettingsService.KEY_SESSION_TIMEOUT_MINUTES));
        dto.setDefaultTimezone(map.get(SettingsService.KEY_DEFAULT_TIMEZONE));
        dto.setMaxStudentsPerExam(map.get(SettingsService.KEY_MAX_STUDENTS_PER_EXAM));
        dto.setEnableSystemLogs(map.get(SettingsService.KEY_ENABLE_SYSTEM_LOGS));
        dto.setAlertThreshold(map.get(SettingsService.KEY_ALERT_THRESHOLD));
        dto.setEmailDigestFrequency(map.get(SettingsService.KEY_EMAIL_DIGEST_FREQUENCY));
        dto.setSmsAlertsEnabled(map.get(SettingsService.KEY_SMS_ALERTS_ENABLED));
        dto.setAutoReportIncidents(map.get(SettingsService.KEY_AUTO_REPORT_INCIDENTS));
        dto.setRequireInstitutionApproval(map.get(SettingsService.KEY_REQUIRE_INSTITUTION_APPROVAL));
        dto.setAutoSuspendInactive(map.get(SettingsService.KEY_AUTO_SUSPEND_INACTIVE));
        dto.setMaxAdminsPerInstitution(map.get(SettingsService.KEY_MAX_ADMINS_PER_INSTITUTION));
        return dto;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(SettingsService.KEY_SYSTEM_NAME, systemName);
        map.put(SettingsService.KEY_ADMIN_CONTACT_EMAIL, adminContactEmail);
        map.put(SettingsService.KEY_SESSION_TIMEOUT_MINUTES, sessionTimeoutMinutes);
        map.put(SettingsService.KEY_DEFAULT_TIMEZONE, defaultTimezone);
        map.put(SettingsService.KEY_MAX_STUDENTS_PER_EXAM, maxStudentsPerExam);
        map.put(SettingsService.KEY_ENABLE_SYSTEM_LOGS, enableSystemLogs);
        map.put(SettingsService.KEY_ALERT_THRESHOLD, alertThreshold);
        map.put(SettingsService.KEY_EMAIL_DIGEST_FREQUENCY, emailDigestFrequency);
        map.put(SettingsService.KEY_SMS_ALERTS_ENABLED, smsAlertsEnabled);
        map.put(SettingsService.KEY_AUTO_REPORT_INCIDENTS, autoReportIncidents);
        map.put(SettingsService.KEY_REQUIRE_INSTITUTION_APPROVAL, requireInstitutionApproval);
        map.put(SettingsService.KEY_AUTO_SUSPEND_INACTIVE, autoSuspendInactive);
        map.put(SettingsService.KEY_MAX_ADMINS_PER_INSTITUTION, maxAdminsPerInstitution);
        return map;
    }

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }
    public String getAdminContactEmail() { return adminContactEmail; }
    public void setAdminContactEmail(String adminContactEmail) { this.adminContactEmail = adminContactEmail; }
    public String getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(String sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }
    public String getDefaultTimezone() { return defaultTimezone; }
    public void setDefaultTimezone(String defaultTimezone) { this.defaultTimezone = defaultTimezone; }
    public String getMaxStudentsPerExam() { return maxStudentsPerExam; }
    public void setMaxStudentsPerExam(String maxStudentsPerExam) { this.maxStudentsPerExam = maxStudentsPerExam; }
    public String getEnableSystemLogs() { return enableSystemLogs; }
    public void setEnableSystemLogs(String enableSystemLogs) { this.enableSystemLogs = enableSystemLogs; }
    public String getAlertThreshold() { return alertThreshold; }
    public void setAlertThreshold(String alertThreshold) { this.alertThreshold = alertThreshold; }
    public String getEmailDigestFrequency() { return emailDigestFrequency; }
    public void setEmailDigestFrequency(String emailDigestFrequency) { this.emailDigestFrequency = emailDigestFrequency; }
    public String getSmsAlertsEnabled() { return smsAlertsEnabled; }
    public void setSmsAlertsEnabled(String smsAlertsEnabled) { this.smsAlertsEnabled = smsAlertsEnabled; }
    public String getAutoReportIncidents() { return autoReportIncidents; }
    public void setAutoReportIncidents(String autoReportIncidents) { this.autoReportIncidents = autoReportIncidents; }
    public String getRequireInstitutionApproval() { return requireInstitutionApproval; }
    public void setRequireInstitutionApproval(String requireInstitutionApproval) { this.requireInstitutionApproval = requireInstitutionApproval; }
    public String getAutoSuspendInactive() { return autoSuspendInactive; }
    public void setAutoSuspendInactive(String autoSuspendInactive) { this.autoSuspendInactive = autoSuspendInactive; }
    public String getMaxAdminsPerInstitution() { return maxAdminsPerInstitution; }
    public void setMaxAdminsPerInstitution(String maxAdminsPerInstitution) { this.maxAdminsPerInstitution = maxAdminsPerInstitution; }
}
