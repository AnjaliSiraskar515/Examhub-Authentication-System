# ExamHub — Recent Features Documentation
### Final Year Project | Secure Exam Hall Disclosure System

---

## Table of Contents
1. [Secure QR Code on Hall Ticket](#1-secure-qr-code-on-hall-ticket)
2. [Hall Allocation System](#2-hall-allocation-system)
3. [Exam Creation (University Admin)](#3-exam-creation-university-admin)
4. [Automated Notification System](#4-automated-notification-system)
5. [Hall Ticket (Admit Card) Redesign](#5-hall-ticket-admit-card-redesign)
6. [Exam Status Auto-Updater](#6-exam-status-auto-updater)
7. [Student Dashboard — Upcoming Schedule Fix](#7-student-dashboard--upcoming-schedule-fix)
8. [Center Name Fixes Across Dashboards](#8-center-name-fixes-across-dashboards)

---

## 1. Secure QR Code on Hall Ticket

### 🧑‍💼 Non-Technical Explanation
Previously, the QR code printed on a student's hall ticket contained the actual exam hall name (e.g., "Computer Lab-01") inside it. Any student could scan this QR with their phone camera and **immediately see which hall they were assigned to** — defeating the entire purpose of keeping the hall secret until 2 hours before the exam.

We fixed this completely. Now, the QR code on the hall ticket contains a **meaningless random-looking code** such as:
```
EXAMHUB-1017-4F3A9C2B1D8E7F6A
```
This code reveals nothing. A student scanning it will only see this code, which is completely useless on its own.

### 🔧 Technical Explanation
**Before:**
```
QR contained: VERIFY_HASH:abc123|REG_ID:1017|SEAT:A-001|HALL:Computer Lab-01
```
Plain text inside the QR — any free QR scanner app could decode this.

**After:**
```
QR contains ONLY: EXAMHUB-{registrationId}-{SHA-256 hash}
```

**How it works:**
- A SHA-256 cryptographic hash is computed from `registrationId + studentEmail + secret_salt` combined together.
- Only the first 16 characters of this hash are used as the token.
- The QR stores nothing readable — just this opaque token.
- A new supervisor-only API endpoint was created:
  ```
  GET /api/admit-card/verify/{token}
  ```
- This endpoint is **protected by Spring Security** — only users with the `SUPERVISOR` role can call it.
- When a supervisor scans the QR and calls this endpoint with their JWT token, they get the full student details: name, hall, seat, and eligibility status.

**Files changed:**
- `AdmitCardService.java` — QR payload generation
- `AdmitCardController.java` — New `/verify/{token}` endpoint

---

## 2. Hall Allocation System

### 🧑‍💼 Non-Technical Explanation
The hall allocation system is how students get assigned to specific exam halls and seats before the exam. A University Admin or Head Supervisor can create exam halls (like "Computer Lab-01", "Seminar Hall A"), add seats to them, and then the system automatically assigns each registered student to a seat in a specific hall.

The student does NOT see which hall they are in on their hall ticket. They only find out **2 hours before the exam** when they receive an in-app notification.

### 🔧 Technical Explanation
**Models involved:**
- `ExamHall` — Stores hall name, capacity, exam ID, and assigned supervisor ID.
- `ExamSeatAllocation` — Stores which student (by `registrationId`) is assigned to which hall and seat number.

**Database tables:** `exam_halls`, `exam_seat_allocations`

**APIs:**
| Endpoint | Purpose |
|---|---|
| `POST /api/halls` | Create a new exam hall |
| `GET /api/halls/exam/{examId}` | Get all halls for an exam |
| `POST /api/halls/{hallId}/allocate` | Assign students to seats in a hall |

**Allocation logic:**
- When allocating, the system loops through all `APPROVED` registrations for that exam.
- Seats are generated sequentially (A-001, A-002, etc.).
- If the hall is full, overflow goes to the next hall.
- Each `ExamSeatAllocation` record links: `registrationId → hallId → seatNumber`.

**Files involved:**
- `ExamHallRepository.java`, `ExamSeatAllocationRepository.java`
- `SupervisorHallController.java`
- `university/allocation.js` (frontend)

---

## 3. Exam Creation (University Admin)

### 🧑‍💼 Non-Technical Explanation
The University Admin can create exams through a step-by-step wizard on their dashboard. They fill in details like the exam name, course, department, semester, which subjects are included, and set the exam date, start time, and end time. They also specify the exam center (college name) and assign supervisors to the exam.

The system also automatically shows only the exams that belong to that university admin's college — so admins from different colleges don't see each other's exams.

### 🔧 Technical Explanation
**Main model:** `UniversityExam` (table: `new_university_exams`)

**Key fields:**
```java
private String sessionName;       // Exam name/title
private String course;            // e.g., "B.Tech"
private String department;        // e.g., "Computer Engineering"
private String semester;          // e.g., "6"
private String institutionCode;   // Scoped per admin's college
private String centerName;        // Exam center/college display name
private ExamSchedule schedule;    // Embedded: examDate, startTime, endTime
private String status;            // DRAFT → OPEN → LIVE → COMPLETED
```

**Exam scoping by institution:**
- When a University Admin creates an exam, their `institutionCode` is automatically attached to it from their JWT profile.
- The `GET /api/university/exams` endpoint filters exams by `institutionCode` — so each admin only sees their own college's exams.
- Students and supervisors see exams based on their `collegeId` match.

**Status lifecycle:**
```
DRAFT → OPEN (published) → LIVE (auto, during exam) → COMPLETED (auto, after exam)
```

**Files involved:**
- `UniversityExam.java`, `ExamSchedule.java` (models)
- `UniversityExamController.java`, `UniversityExamService.java`
- `university/exams.js`, `university/exam-wizard.js` (frontend)

---

## 4. Automated Notification System

### 🧑‍💼 Non-Technical Explanation
The system automatically sends **two types of notifications** before each exam, without any manual intervention:

1. **Students (2 hours before):** Each approved student receives an in-app notification telling them which exam hall they are in and their seat number. This is the secure disclosure — students only find out the hall at this point.

2. **Supervisors (3 hours before):** Each supervisor assigned to a hall receives a duty reminder telling them which hall to report to and when.

Both notifications run automatically in the background every 5 minutes. If an exam is scheduled for 10 PM, students get their hall notification at 8 PM and supervisors get theirs at 7 PM.

### 🔧 Technical Explanation
**Class:** `ExamStatusScheduler.java` (runs as a Spring `@Component`)

**Three scheduled methods (all run every 5 minutes):**

#### Method 1: `updateExamStatuses()`
- Scans all non-completed exams.
- Uses `LocalDateTime` (not just `LocalDate`) for precise comparison.
- Handles **cross-midnight exams** (e.g., 10 PM to 1 AM) by detecting when `endTime < startTime` and adding 1 day to `endDateTime`.
- Sets status to `LIVE` when `now ≥ startTime`, `COMPLETED` when `now ≥ endTime`.

#### Method 2: `notifyStudentsOfHall()`
- Runs 2 hours before the exam start (`startDateTime.minusHours(2)`).
- Finds all `APPROVED` `ExamRegistration` records for that exam.
- Fetches the `ExamSeatAllocation` for each registration to get hall name and seat.
- Calls `NotificationService.createNotification(studentId, title, message)`.
- Uses an in-memory `Set<Long> studentNotifiedExams` to prevent duplicate notifications across scheduler runs.

#### Method 3: `notifySupervisorsOfDuty()`
- Runs 3 hours before the exam start.
- Fetches all `ExamHall` records for the exam.
- Notifies each hall's assigned supervisor (`hall.getExamSupervisorId()`).
- Also separately notifies the head supervisor (`exam.getSupervisorId()`).
- Uses `Set<Long> supervisorNotifiedExams` to deduplicate.

> **Bug Fixed:** The old scheduler had a 5-minute window for notifications (e.g., exactly 8:00 PM to 8:05 PM). If the server restarted in that window, notifications were permanently missed. The fix broadens the window to cover the full 2-hour period leading up to the exam.

**Files involved:**
- `ExamStatusScheduler.java`
- `NotificationService.java`

---

## 5. Hall Ticket (Admit Card) Redesign

### 🧑‍💼 Non-Technical Explanation
The hall ticket is the official document students print or show at the exam center for entry. We made two key changes:

1. **"Exam Hall" was replaced with "Exam Center"** — The hall ticket no longer shows the specific room/hall (since that's kept secret). Instead, it shows the **college name** (exam center) so students know where to go. The specific hall inside the college is revealed via notification.

2. **A new instruction was added** at the bottom of the hall ticket informing students that their specific exam hall and room will be communicated via in-app notification 2 hours before the exam.

### 🔧 Technical Explanation
**File changed:** `student_admit_card.html` + inline JavaScript

**Before:**
```html
<td class="label">Exam Hall:</td>
<td class="value"><span id="examHall">Will be notified 2 hrs before exam</span></td>
```

**After:**
```html
<td class="label">Exam Center:</td>
<td class="value"><span id="examCenter">-</span></td>
```

**JavaScript update** to populate the center:
```javascript
document.getElementById('examCenter').innerText =
    data.centerName || data.institutionName || '-';
```

**New instruction added (point #9):**
> Your specific Exam Hall & Room will be communicated via an in-app notification **2 hours before** the exam start time. Please check your notifications on the ExamHub portal before arriving at the center.

**Backend — `AdmitCardService.java`:**
- `dto.setCenterName(actualCenterName)` now correctly resolves to the **college name**, not the specific hall name.
- `actualCenterName` is derived from the seat allocation's center field first, then falls back to the institution name from the exam record.

---

## 6. Exam Status Auto-Updater

### 🧑‍💼 Non-Technical Explanation
The status of an exam (Upcoming, Live, Completed) automatically updates across all dashboards without anyone needing to manually change it.

- **Before the exam:** Status shows as `UPCOMING`
- **When the exam starts:** Status automatically changes to `LIVE` — visible on the Student Dashboard, Supervisor Dashboard, University Admin Dashboard, and Head Supervisor Dashboard.
- **After the exam ends:** Status automatically changes to `COMPLETED`.

The system checks every 5 minutes and updates the database accordingly.

A special case was fixed for **late-night exams** (like 10 PM to 1 AM). Previously the system would incorrectly mark the exam as `COMPLETED` at midnight because a new calendar day started. Now it correctly understands the exam runs from 10 PM on Day 1 to 1 AM on Day 2.

### 🔧 Technical Explanation
**Root cause of the cross-midnight bug:**
```java
// OLD — broken for cross-midnight exams
if (examDate.isBefore(today)) {
    exam.setStatus("COMPLETED"); // Midnight trigger was wrong!
}
```

**Fix — use full `LocalDateTime`:**
```java
LocalDateTime startDateTime = LocalDateTime.of(examDate, startTime);
LocalDateTime endDateTime   = LocalDateTime.of(examDate, endTime);

// Cross-midnight detection
if (endTime.isBefore(startTime)) {
    endDateTime = endDateTime.plusDays(1); // e.g., 1 AM becomes next day
}

if (now.isBefore(startDateTime))           { /* do nothing — upcoming */ }
else if (now.isBefore(endDateTime))        { exam.setStatus("LIVE"); }
else                                       { exam.setStatus("COMPLETED"); }
```

**Default end time:** If the exam has no `endTime` set, the system defaults to `startTime + 3 hours`.

**Database persistence:** Every status update is immediately saved via `universityExamRepository.save(exam)`, so all dashboards reading from the database see the correct status instantly.

---

## 7. Student Dashboard — Upcoming Schedule Fix

### 🧑‍💼 Non-Technical Explanation
The Student Dashboard has an "Upcoming Schedule" section that was showing "TBD" for everything — the date, time, and venue were all blank. This was fixed so it now correctly shows the exam name, date, formatted time range (e.g., "10:00 PM to 01:00 AM"), and the college name as the venue.

### 🔧 Technical Explanation
**Root cause:** The dashboard was fetching from the **wrong API endpoint**.

```javascript
// OLD — fetching legacy exam records with wrong field names
const examsResponse = await fetch('/api/exam/all', ...);
const exam = exams.find(e => e.examId === r.examId); // examId doesn't match
const date = exam.examDate;    // field doesn't exist in new model
const time = exam.startTime;   // field doesn't exist in new model
```

**Fix:**
```javascript
// NEW — fetching correct UniversityExam records
const examsResponse = await fetch('/api/university/exams', ...);
const exam = exams.find(e => e.id === r.examId); // correct ID field
const date = exam.schedule.examDate;              // correct nested field
const time = exam.schedule.startTime;             // correct nested field
```

**Time formatting fix:**
```javascript
// Format raw "22:00:00" → "10:00 PM"
const fmtTime = t => t ?
    (parseInt(t.split(':')[0]) % 12 || 12) + ':' + t.split(':')[1] +
    ' ' + (parseInt(t.split(':')[0]) >= 12 ? 'PM' : 'AM') : 'TBD';

// Calculate end time if not stored (default: start + 3 hours)
const duration = exam.durationMinutes || 180;
const end = new Date(start.getTime() + duration * 60000);
const calcETime = end.toLocaleTimeString('en-US', { hour12: true });

time = `${sTime} to ${calcETime}`;  // e.g., "10:00 PM to 01:00 AM"
```

**Files changed:** `DashboardHome.js`, `MyRegistrations.js`

---

## 8. Center Name Fixes Across Dashboards

### 🧑‍💼 Non-Technical Explanation
Several places in the student-facing dashboards were incorrectly showing "Center Not Assigned" or a wrong default college name instead of the actual college (Dhole Patil College of Engineering).

We fixed the display logic so that the correct college name is always shown by checking multiple data sources in priority order.

### 🔧 Technical Explanation
**Priority waterfall for center name (frontend):**
```javascript
const centerName =
    (exam?.centerName && exam.centerName !== 'N/A')     ? exam.centerName :
    (reg.institutionName && reg.institutionName !== 'N/A') ? reg.institutionName :
    (profile?.collegeName && profile.collegeName !== 'N/A') ? profile.collegeName :
    'Dhole Patil College of Engineering'; // final fallback
```

**Backend fix in `AdmitCardService.java`:**
The `centerName` field was accidentally being set to `hallName` (the specific room) instead of the college-level center name. Fixed by introducing `actualCenterName`:
```java
String actualCenterName = (seatAlloc != null && seatAlloc.getCenterName() != null)
    ? seatAlloc.getCenterName()
    : buildCenterName(registration); // derives from exam's institutionName

dto.setCenterName(actualCenterName); // was wrongly: dto.setCenterName(hallName)
```

**`ProfileController.java`:**
The `/api/profile/info` endpoint now returns `collegeName` from `user.getCollege().getName()`, which is used as a final fallback in the frontend center name resolution.

---

## Summary Table

| Feature | Who Benefits | Type |
|---|---|---|
| Secure opaque QR token | Students, Supervisors | Security |
| Hall allocation + seat assignment | Admin, Supervisors | Core Feature |
| Exam creation wizard (scoped per college) | University Admin | Core Feature |
| 2-hour student hall notification | Students | Automation |
| 3-hour supervisor duty notification | Supervisors | Automation |
| Hall ticket shows Exam Center (not hall) | Students | UI/UX |
| QR verify endpoint (supervisor-only) | Supervisors | Security + API |
| LIVE/COMPLETED auto status | All dashboards | Automation |
| Cross-midnight exam support | All dashboards | Bug Fix |
| Upcoming Schedule time/date fix | Students | Bug Fix |
| Center name priority fallback | Students | Bug Fix |

---

*Documentation generated: May 2026 | ExamHub Final Year Project*
