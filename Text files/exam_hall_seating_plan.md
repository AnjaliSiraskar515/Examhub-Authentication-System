# 🎓 Exam Hall & Seating Plan — Complete System Design

> **Context:** Final Year Project — Exam Authentication System  
> **Scope:** Hall Assignment + Seat Allocation + Roll Number + Multi-College University Exam  
> **Goal:** Plan only — no implementation yet

---

## 🔍 The Real Problems You've Identified

You have raised **two deeply connected problems**. Let me state them clearly first.

---

### Problem 1 — Who conducts an exam at which college?

Currently, `UniversityExam` is published by the University Admin and has:
- `collegeId` — one college linked
- `supervisorId` + `supervisorIds` — one or more supervisors

**Reality of a university exam:**
> The university publishes ONE exam (e.g., "B.Tech Sem 6 May 2026") that applies to ALL affiliated colleges under it. Each college will run the exam on the same day, for the same students of that college, supervised by its own Head Supervisor.

**So your current model is BROKEN for this.** One exam record cannot map to all colleges correctly.

---

### Problem 2 — Seats, Halls, and Roll Numbers

Currently `ExamCenterAllocation` exists but is **completely empty (all TODOs)**. The `AdmitCardService` generates seat numbers as `S00000001` (just the registration ID), completely ignoring halls and rooms. The `ExamCenterAllocation` table is never populated or used.

---

## ✅ THE PLAN — Step by Step

---

## PART A — Fix the University Exam Multi-College Problem

### The Core Concept: `ExamCollegeMapping` Table

A university exam should fan out to every college. Each college gets its **own context** — its own supervisor, its own hall configuration, its own seat numbers — but all under the **same parent exam**.

```
UniversityExam (parent)
    ├── ExamCollegeMapping (College A → Head Supervisor X)
    ├── ExamCollegeMapping (College B → Head Supervisor Y)
    └── ExamCollegeMapping (College C → Head Supervisor Z)
```

### New Table: `exam_college_mappings`

| Column | Type | Purpose |
|---|---|---|
| `id` | BIGINT PK | Auto |
| `exam_id` | BIGINT FK → `new_university_exams` | Which exam |
| `college_id` | BIGINT FK → `colleges` | Which college runs it |
| `head_supervisor_id` | BIGINT FK → `users` | Who supervises this college |
| `status` | VARCHAR | `PENDING`, `READY`, `COMPLETED` |
| `created_at` | DATETIME | Auto |

### Flow Change:

**University Admin creates exam →**
- Still fills the same exam wizard form
- But at the end (Step: Supervisor Assignment) instead of picking ONE supervisor, they see a **list of their registered colleges** with a dropdown per college to assign its Head Supervisor
- On submit: one `UniversityExam` row + multiple `ExamCollegeMapping` rows are saved

**Head Supervisor sees exam →**
- Currently: He sees exams where `supervisorId` = his ID
- New: He sees exams where `ExamCollegeMapping.head_supervisor_id` = his ID
- He knows which college he's responsible for

This **also fixes** the question of "on which basis do we assign different colleges to different supervisors?" — The university admin explicitly does this per exam, and the Head Supervisor's assignment is already captured in the `college_id` field of the User record.

---

## PART B — Hall & Room Configuration (Head Supervisor's Role)

Once an `ExamCollegeMapping` is created for a college, the **Head Supervisor** of that college is responsible for configuring the physical exam logistics.

### New Table: `exam_halls`

| Column | Type | Purpose |
|---|---|---|
| `id` | BIGINT PK | Auto |
| `exam_id` | BIGINT FK | Which exam |
| `college_id` | BIGINT FK | Which college |
| `hall_name` | VARCHAR | e.g., `Hall A`, `Lab 101` |
| `capacity` | INT | Number of seats in this hall |
| `created_by_supervisor_id` | BIGINT FK → users | Which head supervisor added this |

### Head Supervisor's Workflow:

1. He opens Communication Hub → sees the exam assigned to him
2. He opens a new **"Exam Halls"** section in his dashboard (only visible for mapped exams)
3. He enters halls: Name = "Hall A", Capacity = 40; Name = "Lab 101", Capacity = 25
4. He submits — this creates `exam_halls` rows

**No document upload needed.** Simple form. Clean and structured.

---

## PART C — Seat Allocation & Roll Number Generation (Auto + Admin-Triggered)

After the university admin confirms exam registrations (approves students), a one-click **"Generate Seat Allocation"** button triggers the backend.

### Algorithm (Deterministic & Fair):

```
Input: All APPROVED ExamRegistrations for this exam at this college
Input: All exam_halls configured by the Head Supervisor

1. Sort students by PRN (alphabetical/numeric) → ensures consistent roll number order
2. For each college's student list:
   - Roll number format: <COLLEGE_CODE>/<YEAR>/<SERIAL>
     e.g., ABC/2026/001, ABC/2026/002...
3. Seat assignment (round-robin across halls):
   - Hall A capacity=40, Hall B capacity=30
   - Students 1-40 → Hall A, Seats A-001 to A-040
   - Students 41-70 → Hall B, Seats B-001 to B-030
4. Save each assignment to `exam_seat_allocations` table
```

### New Table: `exam_seat_allocations`

| Column | Type | Purpose |
|---|---|---|
| `id` | BIGINT PK | Auto |
| `registration_id` | BIGINT FK → `exam_registrations` | Which student's registration |
| `exam_id` | BIGINT FK | Which exam |
| `college_id` | BIGINT FK | Which college |
| `hall_id` | BIGINT FK → `exam_halls` | Physical hall assigned |
| `hall_name` | VARCHAR | Denormalized for easy display |
| `seat_number` | VARCHAR | e.g., `A-001`, `B-015` |
| `roll_number` | VARCHAR | e.g., `ABC/2026/001` |
| `generated_at` | DATETIME | When generated |

### Roll Number Validation:
- Roll number = `<institutionCode>/<examYear>/<paddedSerial>`
- Must be unique per exam across all colleges
- Backed by `UNIQUE(exam_id, roll_number)` DB constraint

---

## PART D — Connections to Existing Modules

### Hall Ticket (AdmitCard) — `AdmitCardService.java`

**Current (broken):**
```java
private String buildSeatNumber(ExamRegistration registration) {
    return "S" + String.format("%08d", registration.getId()); // wrong!
}
```

**Fixed flow:**
```java
ExamSeatAllocation alloc = seatAllocationRepo.findByRegistrationId(registration.getId());
dto.setSeatNumber(alloc.getSeatNumber());       // e.g., A-001
dto.setRollNumber(alloc.getRollNumber());        // e.g., ABC/2026/001
dto.setHallName(alloc.getHallName());            // e.g., Hall A
dto.setCenterName(exam.getCenterName());         // college name
```

**Hall ticket template changes:**
- Add `Roll Number` field (new, important!)
- Replace generic seat number with `Hall: A | Seat: A-001`
- Center Name = The college's actual name

### QR Module — `QrService.java`

**Current QR payload:**
```
VERIFY_HASH:<sha256>|REG_ID:<id>
```

**Enhanced QR payload (after seat allocation):**
```
VERIFY_HASH:<sha256>|REG_ID:<id>|ROLL:<rollNumber>|SEAT:<seatNumber>|HALL:<hallName>
```

The supervisor scanning the QR code will now immediately see which hall and seat to direct the student to.

### Supervisor Dashboard — Seating Chart View

The Head Supervisor should see a **hall-wise seating chart** on his dashboard:

```
Hall A (40 seats)
┌─────────────────────────────────────────┐
│ A-001  John Doe         ABC/2026/001    │
│ A-002  Jane Smith       ABC/2026/002    │
│ A-003  Raj Kumar        ABC/2026/003    │
└─────────────────────────────────────────┘
```

This is purely read-only. He can also see which students are unverified (no QR scan yet).

---

## PART E — University Admin Oversight

The University Admin sees:
- All colleges and their allocation status (`PENDING` / `HALLS_CONFIGURED` / `SEATS_GENERATED`)
- Per-college: how many students registered, how many assigned seats, how many halls
- A master button **"Generate All Seat Allocations"** (runs for all colleges at once)
- Or individual college-level button

---

## PART F — Role Summary Matrix

| Role | What They Do |
|---|---|
| **University Admin** | Creates exam, assigns colleges → supervisors, approves registrations, triggers seat generation |
| **Head Supervisor** | Configures exam halls for his college, views seating chart, scans QR during exam |
| **Student** | Registers for exam, gets hall ticket with hall + seat + roll number |

---

## Implementation Order (Priority Sequence)

```
Phase 1 — DB Schema (No Risk)
  ✅ Create exam_college_mappings table
  ✅ Create exam_halls table
  ✅ Create exam_seat_allocations table (extend existing ExamCenterAllocation or replace)

Phase 2 — Backend Services (New, don't touch old)
  ✅ ExamCollegeMappingService: save/fetch mappings
  ✅ ExamHallService: CRUD for halls by Head Supervisor
  ✅ SeatAllocationService: algorithm to generate and save seat allocations

Phase 3 — Backend Controllers (New endpoints)
  ✅ POST /api/university/exam/{examId}/college-mappings  (university admin)
  ✅ POST /api/supervisor/exam/{examId}/halls             (head supervisor)
  ✅ POST /api/university/exam/{examId}/generate-seats    (university admin trigger)
  ✅ GET  /api/supervisor/exam/{examId}/seating-chart     (head supervisor view)

Phase 4 — Frontend Updates
  ✅ University Admin Exam Wizard: add college-supervisor mapping step
  ✅ Head Supervisor Dashboard: add Halls configuration section
  ✅ University Admin Dashboard: add seat generation button + status view
  ✅ Supervisor Dashboard: add seating chart view

Phase 5 — Connect Existing Modules
  ✅ Fix AdmitCardService to read from seat allocations
  ✅ Enhance QR payload to include roll number and hall
  ✅ Validate roll number uniqueness
```

---

## What NOT to Change

- `ExamRegistration`, `User`, `College`, `UniversityExam` — **structure preserved**
- `AdmitCardService` — only add fallback logic, don't rewrite
- `QrService` / `QrController` — only extend QR payload data
- All existing working modules: Student registration, biometric, QR scan — **untouched**

---

## Key Design Decisions

| Decision | Reasoning |
|---|---|
| `ExamCollegeMapping` as new table (not modifying `UniversityExam`) | Zero breaking change risk |
| Hall config by Head Supervisor (not University Admin) | Supervisors know their physical premises best |
| Seat allocation triggered by University Admin (not auto) | Admin controls when hall tickets are released |
| Roll number = `<CollegeCode>/<Year>/<Serial>` | Matches real university patterns |
| Seat number = `<HallPrefix>-<PaddedSerial>` | Easy for supervisors to navigate |
| No document upload for halls | Form-based is faster, cleaner, more reliable |

---

> **Next Step:** Confirm this plan with the user. Once approved, start from Phase 1 (DB schema additions only).
