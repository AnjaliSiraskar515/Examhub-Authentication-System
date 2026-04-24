# Super Admin Dashboard & System Settings Status

This document describes what is implemented, what is partially implemented, what is pending, and why.  
It also explains how each area affects external/real exam operations.

---

## 1) Scope Covered

- Super Admin Dashboard (`frontend/super_admin_dashboard.html`)
- System Settings backend integration
- Notifications panel integration
- Maintenance actions (backup/cache)
- Governance rule configuration

---

## 2) System Settings Module Status

### 2.1 Backend Core (Implemented)

Implemented:

- `SystemSetting` entity (`system_settings` table)
- `SystemSettingRepository`
- `SettingsService`
- `SystemSettingsController`
- Secure endpoints (`SUPER_ADMIN` / `SUPERADMIN`)
- Audit logging for settings updates (`SYSTEM_SETTINGS_UPDATED`)

APIs:

- `GET /api/admin/settings`
- `PUT /api/admin/settings`
- `GET /api/admin/settings/meta`
- `GET /api/admin/settings/typed`
- `PUT /api/admin/settings/typed`

Result:

- Settings are persisted in DB, reloaded on refresh, and audited.

---

### 2.2 Field-by-Field Status (System Settings Tabs)

#### General

- `System Name`  
  - Status: **Stored and reloads correctly**
  - Runtime impact: currently config-level metadata

- `Admin Contact Email`  
  - Status: **Stored and reloads correctly**
  - Runtime impact: used as platform contact config

- `Session Timeout (Minutes)`  
  - Status: **Fully wired**
  - Runtime impact: used in JWT expiration in `JwtUtil`
  - External exam impact: controls forced re-login window / session security

- `Max Students Per Exam`  
  - Status: **Fully wired**
  - Runtime impact: enforced in exam registration validation
  - External exam impact: prevents over-allocation/over-capacity

- `Default Timezone`  
  - Status: **Removed from UI by request**
  - Backend key still exists for compatibility; not shown in current UI

- `Enable System Logs`  
  - Status: **Removed from UI by request**
  - Backend key still exists for compatibility; not shown in current UI

#### Notifications

- `Real-time Alert Threshold`  
  - Status: **Stored and reloads correctly**
  - Runtime impact: currently policy/config level (not fully driving outbound delivery engine yet)

- `Enable SMS Alerts for Admin`  
  - Status: **Stored and reloads correctly**
  - Runtime impact: config ready; SMS dispatcher enforcement is pending

- `Auto-Report Security Incidents to Chief`  
  - Status: **Stored and reloads correctly**
  - Runtime impact: config ready; auto-escalation job integration is pending

- `Email Digest Frequency`  
  - Status: **Removed from UI by request**
  - Backend key still exists for compatibility

#### Maintenance

- `Backup Now`  
  - Status: **Working**
  - Runtime impact:
    - Generates JSON backup in `backend/backups/`
    - Includes metadata, settings snapshot, table stats, and table-level exported data
  - External exam impact: recovery and traceability support

- `Configure Schedule`  
  - Status: **Working**
  - Runtime impact: stores `maintenance.backup.schedule` (Hourly/Daily/Weekly/Monthly)
  - Note: schedule value is persisted; automatic timed execution scheduler is not added yet

- `Clear Cache`  
  - Status: **Working**
  - Runtime impact: clears Spring application caches via `CacheManager`
  - Note: does not clear DB/OS/browser caches

- `Cache Size`  
  - Status: **Working (dynamic estimate)**
  - Runtime impact: loaded from backend cache status endpoint
  - Note: memory size is approximate, based on entry estimation

- `View Full Logs`  
  - Status: **Working**
  - Runtime impact: fetches and shows latest admin/supervisor action logs

#### Governance

- `Require Institution Approval`  
  - Status: **Fully wired**
  - Runtime impact: onboarding becomes pending vs auto-approved
  - External exam impact: controls quality gate for new institutions

- `Auto-Suspend Inactive`  
  - Status: **Config saved only (not fully enforced)**
  - Reason pending: requires scheduled inactivity scanning job + policy thresholds

- `Max Admins Per Institution`  
  - Status: **Config saved only (not fully enforced)**
  - Reason pending: requires enforcing checks in institution/admin creation flows

---

## 3) Super Admin Dashboard Areas Outside Settings

### Overview

- KPI cards load from backend reports
- Security/Biometric display was normalized in UI for requested display constraints
- Top-right action changed from Audit Report to Notifications panel

Status: **Working UI + backend data integration**

### Notifications Panel (Top-right button)

- Backend API created: `GET /api/admin/notifications`
- Shows live notification items based on:
  - pending institution requests
  - unread alerts
  - escalated fraud cases
  - backup schedule status
  - latest alert summaries

Status: **Working end-to-end**

### Reports

- Existing reports/summary flows remain functional
- Biometric failure display aligned with overview display policy

Status: **Working with UI-level display alignment**

---

## 4) Why Some Parts Were Not Fully Implemented Yet

These were intentionally left as configuration-only (not forced behavior) to avoid risking stable modules:

1. Auto-suspend inactive institutions  
   - Needs background scheduler + inactivity definition + exemption logic.

2. Max admins per institution hard enforcement  
   - Needs insertion/update hooks in all admin provisioning paths.

3. Notification outbound engine (SMS/email digest dispatch)  
   - Requires queue/scheduler/integration provider, retry policy, and delivery audit model.

These are higher-impact cross-cutting changes and were not forced immediately to avoid breaking currently working flows.

---

## 5) External Exam Impact Summary

What already helps external exams now:

- Session timeout policy is centrally controlled.
- Registration capacity can be controlled (`max students per exam`).
- Institution onboarding gate can be tightened (`require approval`).
- Backup and cache operations are now operational from dashboard.
- Admin notifications are centralized in one panel.

What should be added next for stronger external exam operations:

- Real scheduled backup runner (cron job) using saved schedule value.
- Auto-suspend workflow for inactive institutions.
- Hard cap enforcement for admins per institution.
- Outbound notification dispatcher (SMS/email) controlled by settings.
- DR restore playbook for generated backup artifacts.

---

## 6) Operational Recommendation (Next Phase)

Priority order for production-hardening:

1. Add scheduler for backup + auto-suspend tasks  
2. Enforce max-admin cap in creation/update APIs  
3. Add notification delivery worker (with retries and logs)  
4. Add backup restore verification pipeline in staging

---

## 7) Quick Status Legend

- **Implemented** = fully wired and active in runtime
- **Partial** = persisted/configured but not enforced by worker/hook everywhere
- **Pending** = not implemented yet to avoid risk to stable modules

