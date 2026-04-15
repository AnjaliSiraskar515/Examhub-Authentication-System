package com.example.examauth.controller;

import com.example.examauth.model.Alert;
import com.example.examauth.repo.AlertRepository;
import com.example.examauth.repo.FraudLogRepository;
import com.example.examauth.repo.InstitutionRepository;
import com.example.examauth.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final InstitutionRepository institutionRepository;
    private final AlertRepository alertRepository;
    private final FraudLogRepository fraudLogRepository;
    private final SettingsService settingsService;

    public AdminNotificationController(
            InstitutionRepository institutionRepository,
            AlertRepository alertRepository,
            FraudLogRepository fraudLogRepository,
            SettingsService settingsService) {
        this.institutionRepository = institutionRepository;
        this.alertRepository = alertRepository;
        this.fraudLogRepository = fraudLogRepository;
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> getNotifications() {
        List<Map<String, Object>> notifications = new ArrayList<>();

        long pendingInstitutions = institutionRepository.countByStatusIgnoreCase("pending");
        if (pendingInstitutions > 0) {
            notifications.add(notification(
                    "pending-institutions",
                    "high",
                    pendingInstitutions + " new institution request" + (pendingInstitutions > 1 ? "s" : ""),
                    "New onboarding requests require review.",
                    "Just now",
                    "institutions"));
        }

        long unreadAlerts = alertRepository.countByIsReadFalse();
        if (unreadAlerts > 0) {
            notifications.add(notification(
                    "unread-alerts",
                    "medium",
                    unreadAlerts + " unread security alert" + (unreadAlerts > 1 ? "s" : ""),
                    "Recent exam/security alerts need attention.",
                    "Today",
                    "reports"));
        }

        long escalatedFraud = fraudLogRepository.countByEscalatedToSuperAdminTrue();
        if (escalatedFraud > 0) {
            notifications.add(notification(
                    "fraud-escalations",
                    "high",
                    escalatedFraud + " fraud case" + (escalatedFraud > 1 ? "s" : "") + " escalated",
                    "Escalated fraud logs are pending super admin review.",
                    "Today",
                    "reports"));
        }

        String backupSchedule = settingsService.getSetting(SettingsService.KEY_BACKUP_SCHEDULE, "Daily");
        notifications.add(notification(
                "backup-schedule",
                "low",
                "Backup schedule is set to " + backupSchedule,
                "Verify backup strategy for compliance and recovery.",
                "Today",
                "settings-maintenance"));

        List<Alert> latestAlerts = alertRepository.findTop10ByOrderByTimestampDesc();
        for (int i = 0; i < Math.min(3, latestAlerts.size()); i++) {
            Alert alert = latestAlerts.get(i);
            notifications.add(notification(
                    "alert-" + alert.getId(),
                    "medium",
                    "Exam Alert: " + (alert.getType() == null ? "GENERAL" : alert.getType()),
                    alert.getMessage() == null ? "Alert generated." : alert.getMessage(),
                    alert.getTimestamp() == null ? "Recently" : alert.getTimestamp().toString(),
                    "reports"));
        }

        return ResponseEntity.ok(Map.of("notifications", notifications));
    }

    private Map<String, Object> notification(
            String id,
            String priority,
            String title,
            String description,
            String time,
            String actionTarget) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("priority", priority);
        item.put("title", title);
        item.put("description", description);
        item.put("time", time);
        item.put("actionTarget", actionTarget);
        return item;
    }
}
