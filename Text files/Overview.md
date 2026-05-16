# Exam Authentication System using QR and Biometric Verification Technology
## Complete Technical Project Documentation

**Project Title:** Exam Authentication System using QR and Biometric Verification Technology
**Technology Stack:** Java Spring Boot Â· MySQL Â· HTML/CSS/JavaScript Â· JWT Â· SHA-256 Â· AES-256-GCM Â· BCrypt
**Deployment:** Single-port monolithic (port 8080)
**Architecture:** Multi-college, Role-based, Backlog-aware

---

## 1. INTRODUCTION

The **Exam Authentication System** is a full-stack web application designed to digitize, secure, and automate the entire university examination process. It is built using a **Java Spring Boot** backend that simultaneously serves a static HTML/CSS/JavaScript frontend on a single port (8080). The system handles everything from student creation, subject management, backlog tracking, exam scheduling, supervisor assignment, seat allocation, hall ticket generation with embedded QR codes, to real-time biometric identity verification at the exam hall.

The platform follows a strict multi-college architecture where a single university instance manages multiple affiliated colleges â€” each with their own departments, students, subjects, exams, and supervisors â€” with complete data isolation between colleges.

---

## 2. PROBLEM STATEMENT

Traditional university examination systems suffer from:
- Manual, error-prone student eligibility verification, especially for backlog cases
- Physical hall ticket distribution which can be forged or misplaced
- No automated mechanism to verify student identity at the exam hall entrance
- Fragmented software requiring separate instances per college
- Manual seat allocation causing delays and seating conflicts
- No digital audit trail for supervisor actions or attendance records

There is a critical need for a unified, intelligent platform that automates eligibility determination, generates tamper-proof QR-encoded hall tickets, and enables cryptographic biometric identity verification.

---

## 3. OBJECTIVES

1. Build a multi-college university exam management platform under a single deployment
2. Implement role-based access control for Admin, Student, and Supervisor (HEAD/EXAM) roles
3. Automate exam eligibility using backlog-aware subject intersection logic
4. Generate SHA-256 hashed, single-use, time-expiring QR tokens for hall tickets
5. Implement biometric fingerprint hashing for identity verification at the exam hall
6. Automate PRN-sorted sequential seat allocation across exam halls
7. Send OTP via Gmail SMTP and Fast2SMS for two-factor login verification
8. Generate PDF system intelligence and security reports
9. Maintain a full exam attendance audit log per supervisor action

---

## 4. EXISTING vs PROPOSED SYSTEM

| Aspect | Existing System | Proposed System |
|---|---|---|
| Eligibility Check | Manual record lookup | Automated subject-intersection + backlog query |
| Hall Ticket | Printed paper | Digital with SHA-256 QR token |
| Identity Verification | Visual ID check | SHA-256 biometric hash comparison + QR scan |
| Seat Allocation | Manual arrangement | PRN-sorted sequential algorithm |
| Supervisor Access | Paper lists | JWT-authenticated role-based dashboard |
| Multi-College | Separate systems | Single unified instance with isolated views |
| Attendance | Paper register | Digital `exam_attendance` table with biometric result |
| Security | None | BCrypt passwords + JWT + SHA-256 + AES-256-GCM |

---

## 5. SYSTEM ARCHITECTURE

### 5.1 Overview
The system is a **monolithic single-port application**. Spring Boot's embedded Tomcat serves both the REST API (`/api/**`) and all static frontend files (`/static/**`) on port **8080**. This eliminates CORS issues in production and simplifies deployment.

```
Browser (port 8080)
    â”‚
    â”œâ”€â”€ GET /*.html, /js/**, /css/**   â†’ Spring Boot Static Resource Handler
    â””â”€â”€ GET/POST /api/**               â†’ Spring Boot REST Controllers
                                            â”‚
                                       JWT Auth Filter
                                       (JwtAuthenticationFilter.java)
                                            â”‚
                                       Service Layer
                                            â”‚
                                       JPA Repository Layer
                                            â”‚
                                        MySQL DB
```

### 5.2 Technology Stack â€” Full Detail

| Layer | Technology | Version / Detail |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.x |
| ORM | Spring Data JPA + Hibernate | Auto DDL |
| Security | Spring Security | Method-level `@PreAuthorize` |
| Auth Tokens | JWT (jjwt library) | HS512 signed |
| Password Hashing | BCryptPasswordEncoder | Spring Security |
| QR Hashing | SHA-256 (java.security.MessageDigest) | 64-char hex output |
| Biometric Hashing | SHA-256 via HashUtil.java | Template hash comparison |
| Encryption Utility | AES/GCM/NoPadding | 256-bit key, 12-byte IV, 128-bit tag |
| PDF Generation | OpenPDF (com.lowagie) | A4 report generation |
| Email OTP | JavaMailSender + Gmail SMTP | 6-digit, 5-min expiry |
| SMS OTP | Fast2SMS REST API (bulkV2 route) | 6-digit, 5-min expiry |
| Database | MySQL | Relational, JPA-managed schema |
| Frontend | HTML5 + Vanilla CSS + JavaScript | No framework |
| Build Tool | Maven | pom.xml |
| Deployment | Single JAR (embedded Tomcat) | Port 8080 |

### 5.3 Security Layers

**Layer 1 â€” Password Security:** All user passwords are encoded using `BCryptPasswordEncoder` (Spring Security default, cost factor 10). Plaintext passwords are never stored.

**Layer 2 â€” JWT Authentication:** On login, a JWT token is issued signed with `HS512` algorithm. The token embeds:
- `subject` (email)
- `tokenVersion` (integer â€” incremented on password/email change to invalidate old tokens)
- `issuedAt` and `expiration` (configurable session timeout via `SystemSetting`)

Every protected API request passes through `JwtAuthenticationFilter` which extracts the Bearer token, validates signature and expiry, compares `tokenVersion` with the database record, and injects the user's role into the `SecurityContext`.

**Layer 3 â€” SHA-256 Hashing (`HashUtil.java`):** Used in two critical places:
- **QR Token Generation:** `sha256(studentId + "|" + examId + "|" + timestamp + "|" + SECRET_KEY)`
- **Biometric Template Hashing:** `sha256(fingerprintPayload)` â€” stored as `biometricTemplateHash` in the `users` table

**Layer 4 â€” AES-256-GCM Encryption (`AESGcmUtil.java`):** Utility class for symmetric authenticated encryption. Uses `AES/GCM/NoPadding` cipher with a 12-byte random IV generated via `SecureRandom`, and a 128-bit authentication tag. The IV is prepended to the ciphertext and encoded as Base64-URL. AAD (Additional Authenticated Data) is supported for integrity binding.

**Layer 5 â€” Role-Based Access Control:** `SecurityConfig.java` uses `@EnableMethodSecurity` and path-level matchers:
- `/api/student/**` â†’ `ROLE_STUDENT` or `ROLE_SUPERADMIN`
- `/api/supervisor/**`, `/api/biometric/**` â†’ `ROLE_SUPERVISOR` or `ROLE_SUPERADMIN`
- `/api/admin/**`, `/api/institution/**` â†’ `ROLE_UNIVERSITY_ADMIN` or `ROLE_SUPERADMIN`

### 5.4 WebConfig & Single-Port Serving

`WebConfig.java` implements `WebMvcConfigurer` to map all frontend HTML/JS/CSS routes back to the static directory, ensuring that navigating directly to any dashboard URL does not result in a 404.

---

## 6. MODULE-WISE IMPLEMENTATION

### 6.1 User Management Module
**Controller:** `AuthController.java`, `AdminStudentController.java`, `AdminSupervisorController.java`
**Service:** `UserManagementService.java`, `AuthService.java`

**Student Creation:**
- **Manual:** Admin POSTs student details; password is BCrypt-hashed before saving
- **CSV Bulk Upload:** CSV file is parsed line-by-line. Each row maps to a `User` entity with `role=STUDENT`. PRN is set as unique identifier (indexed: `idx_prn`). Email is also indexed (`idx_email`) for fast lookup during login
- The `studentType` field is set to `REGULAR` by default, overridden to `BACKLOG` when backlog CSV is uploaded

**Supervisor Creation:**
- Admin provides: `name`, `email`, `password`, `supervisorType` (`HEAD` or `EXAM`), `collegeName`, `employeeId`, `designation`
- `supervisorType` field is stored on the `User` entity and drives dashboard-level access differentiation

**Fields on User entity:** `userId`, `name`, `username`, `email`, `prn` (unique), `password` (BCrypt), `role`, `status`, `studentType` (REGULAR/BACKLOG), `tokenVersion`, `feesPaid`, `isEligible`, `examAccessAllowed`, `firstLogin`, `biometricHash`, `biometricEnrolled`, `biometricTemplateHash`, `biometricEnrolledAt`, `biometricLastVerified`, `photoPath`, `aadharPath`, `marks10Path`â€“`marks12Path`, `sem1MarksheetPath`â€“`sem8MarksheetPath`, `phoneNumber`, `department`, `major` (= course), `semester`, `cgpa`, `dob`, `gender`, `supervisorType`, `universityName`, `collegeName`, `college` (FK), `departmentEntity` (FK), `institutionCode`, `designation`, `employeeId`, `qrVerified`, `biometricVerified`

### 6.2 College Management Module
**Controller:** `AdminCollegeController.java`
**Entity:** `College.java` â†’ Table: `colleges`

Fields: `id`, `name`, `code`. Colleges are the top-level scope. Every `User` has a `@ManyToOne` FK to `College`. Every `Exam` has a `collegeId` field. This enforces multi-college data isolation â€” supervisors and students only see records belonging to their own college.

### 6.3 Department Management Module
**Controller:** `AdminDepartmentController.java`
**Entity:** `Department.java` â†’ Table: `departments`

Fields: `id`, `name`, `college` (FK â†’ `colleges`). Departments are linked to colleges. The frontend dynamically filters department dropdowns via `GET /api/admin/departments?collegeId=X` whenever a college is selected.

### 6.4 Subject Management Module
**Controller:** `AdminSubjectController.java`
**Entity:** `Subject.java` â†’ Table: `subjects`

Fields: `id`, `name`, `code`, `semester` (Integer), `course` (String), `departmentEntity` (FK â†’ `departments`).

Unique constraint: `(code, semester, department_id, course)` â€” prevents duplicate subjects. Index on `code` for fast lookup. Subjects are the atomic unit used for both eligibility checking and backlog mapping. `GET /api/admin/subjects?course=BE&semester=6&collegeId=X` returns subjects dynamically for CreateExam dropdowns.

### 6.5 Exam Management Module
**Controllers:** `UniversityController.java`, `AdminController.java`
**Entities:** `Exam.java` (table: `exams`), `UniversityExam.java` (table: `new_university_exams`)

**Two exam models exist:**

`Exam` (Legacy/College-level):
- Fields: `examId`, `examName`, `subjectId` (single FK), `type` (ExamType enum: REGULAR/BACKLOG), `semester`, `collegeId`, `date`, `startTime`, `durationMinutes`, `mode`, `location`, `status`, `supervisorId`, `supervisorIds` (List â€” `@ElementCollection`), `institutionName`

`UniversityExam` (Advanced):
- Fields: `id`, `sessionName`, `academicYear`, `examType`, `mode`, `course`, `department`, `semester`, `subjectIds` (List\<Long\> â€” `@ElementCollection`), `status`, `collegeId`, `institutionCode`, `supervisorId`, `supervisorIds` (List), embedded `RegistrationWindow`, `ExamSchedule`, `FeeStructure`, `ExamControls`, center details, online exam link, `proctoringEnabled`

The `ExamResponseDTO` unifies both models in `GET /api/university/{id}/exam` â€” legacy exams get `sourceId=LEGACY_{id}`, university exams get `sourceId=UNIV_{id}`. Results are sorted by date+time descending. Deletion routes through `sourceId` prefix to target the correct repository.

### 6.6 Exam Eligibility Module
**Service:** `StudentExamEligibilityService.java`
**Endpoint:** Called internally when student requests eligible exams

**Gate Checks (must all pass):**
1. `feesPaid == true` â€” else throw `EligibilityException("Outstanding fees")`
2. `isEligible == true` â€” else throw `EligibilityException("Student marked ineligible")`
3. `examAccessAllowed == true` â€” else throw `EligibilityException("Exam access revoked")`

**Eligibility Decision Tree:**
```
Has active StudentBacklog records (cleared=false)?
â”‚
â”œâ”€â”€ YES â†’ BACKLOG PATH
â”‚    Extract subjectIds from backlog records
â”‚    Query: examRepository.findBacklogExams(subjectIds, BACKLOG, "upcoming")
â”‚    Filter: exam.subjectId must NOT be null
â”‚    Return matching backlog exams only
â”‚
â””â”€â”€ NO â†’ REGULAR PATH
     Does student have college + course + semester?
     â”‚
     â”œâ”€â”€ YES â†’ Strict Subject Mapping
     â”‚    Parse semester as Integer
     â”‚    subjectRepository.findByCourseSemesterAndCollege(course, sem, collegeId)
     â”‚    Extract subject IDs â†’ query regular exams by subject IDs
     â”‚
     â””â”€â”€ NO â†’ Fallback
          examRepository.findRegularExams(semester, REGULAR, "upcoming")
```

### 6.7 Supervisor Module
**Controller:** `SupervisorController.java`
**Type field:** `User.supervisorType` = `"HEAD"` or `"EXAM"`

**HEAD Supervisor Logic:**
- Fetches all exams where `exam.collegeId == supervisor.collegeId`
- Does not require the supervisor to be explicitly listed in `supervisorIds`
- Full view of all exam activity within their college

**EXAM Supervisor Logic:**
- Fetches only exams where `supervisor.userId` is present in `exam.supervisorIds` list
- Restricted to their assigned exams only

**Multi-supervisor per exam:** Both `Exam` and `UniversityExam` contain a `List<Long> supervisorIds` stored via `@ElementCollection` in join tables (`exams_supervisor_ids`, `new_university_exams_supervisor_ids`).

**Supervisor Action Log:** `SupervisorActionLog` entity records every supervisor action with a timestamp for audit.

### 6.8 Backlog System Module
**Controller:** `AdminBacklogController.java`
**Entity:** `StudentBacklog.java` â†’ Table: `student_backlogs`

Fields: `id`, `studentId` (FK â†’ users), `subjectId` (FK â†’ subjects, nullable), `subjectName`, `semester`, `cleared` (boolean, default false). The `getStatus()` derived method returns `"CLEARED"` or `"PENDING"` based on the `cleared` flag.

**CSV Parsing Flow:**
1. Admin uploads backlog CSV file
2. Backend reads CSV line by line (skipping header)
3. Each row: extract `studentEmail`, `subjectId`, `subjectName`, `semester`
4. Look up `User` by email â†’ get `userId`
5. Set `user.studentType = "BACKLOG"`
6. Create and save `StudentBacklog` entity
7. Batch-save all records

**Frontend Display:** Student's backlog dashboard shows a color-coded table with `subjectName`, `semester`, and `status` (PENDING in red, CLEARED in green).

### 6.9 Seat Allocation & Hall Management Module
**Controller:** `AdminController.java` (seat allocation endpoints)
**Entities:** `ExamHall` entity linked to exam, `SeatAllocation` entity

**Seat Allocation Algorithm (Step-by-step):**
1. Fetch all students registered for the exam
2. Sort students ascending by PRN (alphanumeric)
3. Fetch all `ExamHall` records assigned to the exam
4. Initialize: `hallIndex = 0`, `seatNumber = 1`
5. For each student in sorted order:
   - Assign `hall = halls[hallIndex]`, `seat = seatNumber`
   - Create and save `SeatAllocation(studentId, examId, hallId, seatNumber)`
   - Increment `seatNumber`
   - If `seatNumber > hall.capacity` â†’ `hallIndex++`, `seatNumber = 1`
6. When all halls are full: remaining students flagged for overflow

### 6.10 Hall Ticket & QR Token Module
**Service:** `QrService.java`
**Controller:** `QrController.java`
**Entity:** `QrCode.java` â†’ Table: `qr_tokens`

**QR Token Generation â€” Technical Detail:**
```java
// Step 1: Compose raw payload
String rawData = studentId + "|" + examId + "|" + System.currentTimeMillis() + "|" + SECRET_KEY;

// Step 2: SHA-256 hash via HashUtil
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));
// Convert to 64-char lowercase hex string

// Step 3: Persist QrCode entity
qrCode.setHashedToken(hash);          // 64-char hex, stored in DB (length=64)
qrCode.setIssuedAt(LocalDateTime.now());
qrCode.setExpiresAt(now.plusMinutes(10));  // 10-minute expiry window
qrCode.setUsed(false);
```

The frontend receives the `hashedToken` (64-char hex string) and renders it as a QR image using a QR code JavaScript library.

**QR Token Verification â€” Step-by-step:**
1. Supervisor scans QR â†’ `POST /api/qr/verify` with token string
2. Lookup `qr_tokens` by `hashedToken` â†’ not found â†’ 401
3. Check `qr.isUsed()` â†’ already true â†’ reject "Token already used"
4. Check `qr.getExpiresAt().isBefore(now)` â†’ expired â†’ reject
5. All checks pass â†’ set `used = true` â†’ save
6. Fetch `User` by `qr.getStudentId()`
7. Mask Aadhar: show only last 4 chars as `XXXX-XXXX-XXXX`
8. Check `biometricLastVerified` within last 5 minutes â†’ `biometricRecentlyVerified` flag
9. Return `QrVerificationResponse` with: student details (name, PRN, photo, hall, seat), biometric status, institution info

**`qr_tokens` Table Schema:**
```
qrId          BIGINT PK AUTO_INCREMENT
studentId     BIGINT (FK â†’ users)
examId        BIGINT (FK â†’ exams)
hashedToken   VARCHAR(64) NOT NULL
issuedAt      DATETIME
expiresAt     DATETIME
used          BOOLEAN DEFAULT false
```

### 6.11 Biometric Verification Module
**Controller:** `BiometricController.java`
**Endpoint:** `POST /api/biometric/verify` (requires ROLE_SUPERVISOR JWT)

**Technical Implementation:**
```
Request Body: { studentId, fingerprint (raw/base64), examId }

Step 1: Extract fingerprint payload
Step 2: templateHash = SHA-256(fingerprint)  [via HashUtil.sha256()]
Step 3: Load User from DB by studentId
Step 4: Read user.biometricTemplateHash

IF biometricTemplateHash == null (first time):
    â†’ ENROLLMENT
    user.biometricTemplateHash = templateHash
    user.biometricEnrolled = true
    user.biometricEnrolledAt = now
    user.biometricLastVerified = now
    user.biometricVerified = true
    ExamAttendance(PRESENT, SUCCESS) saved
    Response: score=100.0, "Enrolled and verified"

ELSE IF SHA-256(incoming) == stored hash:
    â†’ MATCH
    user.biometricLastVerified = now
    user.biometricVerified = true
    ExamAttendance(PRESENT, SUCCESS) saved
    Response: score=98.5, "Biometric Match Confirmed"

ELSE:
    â†’ MISMATCH
    user.biometricVerified = false
    ExamAttendance(ABSENT, FAILED) saved
    Response: HTTP 401
```

Attendance is only written once per `(studentId, examId)` pair (UNIQUE constraint on `exam_attendance` table). Duplicate scan attempts return `attendanceAlreadyRecorded=true`.

### 6.12 OTP Module
**Service:** `OtpService.java`
**Controller:** `OtpController.java`
**Entity:** `OtpEntity` â†’ Table: `otp_entities`

**Email OTP:**
- 6-digit random OTP: `100000 + new Random().nextInt(900000)`
- Stored with 5-minute expiry
- Sent via `JavaMailSender` â†’ Gmail SMTP (`spring.mail.username` from properties)
- Old OTPs for same email deleted before new one is stored (prevents replay)
- Verification: stream filter for `otp.equals(input) && expiryTime.isAfter(now)`

**SMS OTP (Fast2SMS):**
- Phone number cleaned to 10 digits (strips country code)
- Hits `https://www.fast2sms.com/dev/bulkV2` REST endpoint
- Uses `authorization` header with API key from properties
- Route: `q` (quick SMS), language: `english`
- Falls back gracefully if API key not configured (logs OTP to console for dev)

### 6.13 PDF Report Module
**Service:** `PdfService.java`
**Library:** `com.lowagie` (OpenPDF)

Reports generated (all as `application/pdf`):
- **System Intelligence Report:** Executive summary (exams, users, institutions, QR records, fraud count), Exam Analytics, User Analytics, Security & Compliance (AES-GCM 256-bit noted), Incident Report table
- **Security Incident Report:** Biometric mismatch count, IP conflicts, lockout count
- **Institution Performance Rankings:** Rank, institution name, performance score
- **Detailed Exam Performance Report:** Table of all exams with completion %, status
- **Performance Trends Report:** Avg verification time (1.2s), API error rate (0.01%)
- **CSV Activity Log Export:** Timestamped action log

All PDFs use A4 page format with margin 36pt. Professional header with blue title font, dark gray data cells, footer watermark.

### 6.14 Deployment Architecture
**Entry Point:** `ExamAuthApplication.java` â€” `@SpringBootApplication`
**Config Files:**
- `SecurityConfig.java` â€” filter chain, CORS, BCrypt bean, role matchers
- `WebConfig.java` â€” static resource handler, SPA fallback
- `CorsConfig.java` â€” dev CORS (localhost:5500 allowed for VS Code Live Server)
- `DataSeeder.java` â€” seeds default Super Admin on first startup

**Static serving:** All HTML/CSS/JS in `/src/main/resources/static/` are auto-served by Spring Boot's `ResourceHttpRequestHandler`. `WebConfig` maps `/**` to static resources so refreshing any page works correctly.

### 6.15 Super Admin Module
**Controller:** `InstitutionController.java`, `SystemSettingsController.java`, `FraudController.java`
**Access Level:** `ROLE_SUPERADMIN`
**Core Responsibilities:**
- **Institution Onboarding:** Registering new university/college branches, assigning root `UNIVERSITY_ADMIN` accounts, and generating institution API keys.
- **System Global Settings:** Modifying dynamic configurations (e.g., `SESSION_TIMEOUT_MINUTES`, enabling/disabling biometric enforcement globally) without restarting the server via the `system_settings` table.
- **System Intelligence & Reporting:** Extracting PDF executive summaries encompassing total platform usage, fraud incidents (e.g., biometric mismatch rates), and API performance metrics.
- **Fraud Monitoring:** Full access to the `fraud_logs` table to monitor suspicious activities like IP conflicts, rapid failed QR scans, and biometric spoofing attempts.
- **Data Seeding:** Managed by `DataSeeder.java` which ensures at least one default Super Admin exists upon initial database creation.

---

## 7. DATABASE DESIGN

### 7.1 Table: `users`
Primary table for all roles. Indexed on `prn` and `email`.

| Column | Type | Notes |
|---|---|---|
| userId | BIGINT PK AUTO_INCREMENT | |
| name | VARCHAR | |
| email | VARCHAR (indexed) | Login identifier |
| prn | VARCHAR(20) UNIQUE (indexed) | Student PRN |
| password | VARCHAR | BCrypt hashed |
| role | VARCHAR | STUDENT / SUPERVISOR / UNIVERSITY_ADMIN / SUPERADMIN |
| studentType | VARCHAR(20) DEFAULT 'REGULAR' | REGULAR or BACKLOG |
| tokenVersion | INT DEFAULT 0 | JWT invalidation counter |
| feesPaid | BOOLEAN DEFAULT false | Eligibility gate |
| isEligible | BOOLEAN DEFAULT false | Eligibility gate |
| examAccessAllowed | BOOLEAN DEFAULT false | Eligibility gate |
| biometricEnrolled | BOOLEAN | |
| biometricTemplateHash | VARCHAR(256) | SHA-256 of fingerprint |
| biometricLastVerified | DATETIME | Used in QR response |
| supervisorType | VARCHAR | HEAD or EXAM |
| college_id | BIGINT FK â†’ colleges | |
| department_id | BIGINT FK â†’ departments | |
| photoPath | VARCHAR | File path |
| aadharPathâ€¦sem8MarksheetPath | VARCHAR | Document paths |
| qrVerified | BOOLEAN | |
| biometricVerified | BOOLEAN | |

### 7.2 Table: `colleges`
| Column | Type |
|---|---|
| id | BIGINT PK AUTO_INCREMENT |
| name | VARCHAR |
| code | VARCHAR |

### 7.3 Table: `departments`
| Column | Type |
|---|---|
| id | BIGINT PK |
| name | VARCHAR |
| college_id | BIGINT FK â†’ colleges |

### 7.4 Table: `subjects`
Unique constraint: `(code, semester, department_id, course)`

| Column | Type |
|---|---|
| id | BIGINT PK |
| name | VARCHAR |
| code | VARCHAR (indexed) |
| semester | INT |
| course | VARCHAR |
| department_id | BIGINT FK â†’ departments |

### 7.5 Table: `exams` (Legacy)
| Column | Type |
|---|---|
| examId | BIGINT PK |
| examName | VARCHAR |
| subjectId | BIGINT FK â†’ subjects |
| type | ENUM (REGULAR/BACKLOG) |
| semester | VARCHAR |
| collegeId | BIGINT FK â†’ colleges |
| date | DATE |
| startTime | TIME |
| durationMinutes | INT |
| mode | VARCHAR |
| status | VARCHAR |
| supervisorId | BIGINT |
| institutionName | VARCHAR |

Associated join table: `exams_supervisor_ids (exam_exam_id, supervisor_ids BIGINT)`

### 7.6 Table: `new_university_exams`
| Column | Type |
|---|---|
| id | BIGINT PK |
| sessionName | VARCHAR |
| academicYear | VARCHAR |
| examType | VARCHAR |
| mode | VARCHAR |
| course | VARCHAR |
| department | VARCHAR |
| semester | VARCHAR |
| status | VARCHAR |
| collegeId | BIGINT FK â†’ colleges |
| institutionCode | VARCHAR |
| supervisorId | BIGINT |
| centerName, centerCode, centerCapacity | VARCHAR/INT |
| examLink, proctoringEnabled | VARCHAR/BOOLEAN |
| Embedded: RegistrationWindow | startDate, endDate |
| Embedded: ExamSchedule | examDate, startTime, endTime |
| Embedded: FeeStructure | amount, currency |
| Embedded: ExamControls | allowLateEntry, requireBiometric |

Join tables: `new_university_exams_subject_ids`, `new_university_exams_supervisor_ids`

### 7.7 Table: `student_backlogs`
| Column | Type |
|---|---|
| id | BIGINT PK |
| studentId | BIGINT FK â†’ users |
| subjectId | BIGINT (nullable) FK â†’ subjects |
| subjectName | VARCHAR |
| semester | VARCHAR |
| cleared | BOOLEAN DEFAULT false |

### 7.8 Table: `qr_tokens`
| Column | Type |
|---|---|
| qrId | BIGINT PK |
| studentId | BIGINT FK â†’ users |
| examId | BIGINT FK â†’ exams |
| hashedToken | VARCHAR(64) NOT NULL |
| issuedAt | DATETIME |
| expiresAt | DATETIME |
| used | BOOLEAN DEFAULT false |

### 7.9 Table: `exam_attendance`
Unique constraint: `(student_id, exam_id)` â€” one record per student per exam.

| Column | Type |
|---|---|
| id | BIGINT PK |
| student_id | BIGINT FK â†’ users |
| exam_id | BIGINT FK â†’ exams |
| supervisor_id | BIGINT FK â†’ users |
| status | VARCHAR (PRESENT/ABSENT) |
| biometricResult | VARCHAR (SUCCESS/FAILED) |
| verifiedAt | DATETIME |

### 7.10 Table: `exam_registrations`
| Column | Type |
|---|---|
| id | BIGINT PK |
| studentId | BIGINT FK â†’ users |
| examId | BIGINT FK â†’ exams |
| registrationStatus | ENUM (PENDING/APPROVED/REJECTED) |
| hallTicketReleased | BOOLEAN DEFAULT false |

### 7.11 Table: `otp_entities`
| Column | Type |
|---|---|
| id | BIGINT PK |
| email | VARCHAR (nullable) |
| phone | VARCHAR (nullable) |
| otp | VARCHAR(6) |
| expiryTime | DATETIME |
| verified | BOOLEAN |

### 7.12 Other Tables
- `alerts` â€” system alerts with severity/message/read status
- `fraud_logs` â€” fraud incident records
- `incident_reports` â€” supervisor-raised incidents
- `supervisor_action_logs` â€” audit trail of all supervisor actions
- `communication_messages` â€” internal messaging between users
- `system_settings` â€” key-value store (e.g., `SESSION_TIMEOUT_MINUTES`)
- `institutions` â€” institution onboarding records
- `qr_code_entries` â€” additional QR tracking

### 7.13 Entity Relationships Summary

```
University (logical)
    â””â”€â”€ College (1)
            â”œâ”€â”€ Department (Many)
            â”‚       â””â”€â”€ Subject (Many)  â† course + semester + dept
            â”œâ”€â”€ User/Student (Many)     â† college_id FK
            â”‚       â””â”€â”€ StudentBacklog (Many) â† studentId FK
            â”œâ”€â”€ User/Supervisor (Many)  â† college_id FK
            â””â”€â”€ Exam (Many)             â† collegeId FK
                    â”œâ”€â”€ supervisorIds (Many-to-Many via element collection)
                    â”œâ”€â”€ subjectIds (Many via element collection)
                    â”œâ”€â”€ ExamRegistration (Many) â† examId FK
                    â”‚       â””â”€â”€ hallTicketReleased flag
                    â”œâ”€â”€ QrCode/qr_tokens (Many) â† examId FK
                    â””â”€â”€ ExamAttendance (Many)   â† examId FK
```

---

## 8. CORE ALGORITHMS â€” DETAILED

### 8.1 SHA-256 Hashing (`HashUtil.java`)
```java
public static String sha256(String input) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
    // Convert each byte to 2-char hex, zero-padded
    StringBuilder hexString = new StringBuilder();
    for (byte b : encodedHash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
    }
    return hexString.toString(); // 64-char lowercase hex
}
```
**Used for:** QR token generation, biometric fingerprint template storage

### 8.2 AES-256-GCM Encryption (`AESGcmUtil.java`)
```
Algorithm : AES/GCM/NoPadding
Key Size  : 256-bit SecretKey
IV        : 12 bytes, generated via SecureRandom (NIST recommended)
Auth Tag  : 128-bit GCM authentication tag
AAD       : Optional Additional Authenticated Data for integrity binding
Output    : Base64-URL encoded (IV prefix + ciphertext + GCM tag)

Decrypt   : Splits IV from payload, verifies GCM tag, decrypts
```

### 8.3 JWT Token Lifecycle (`JwtUtil.java`)
```
Generate:
  Jwts.builder()
    .setSubject(email)
    .claim("tokenVersion", user.tokenVersion)
    .setIssuedAt(now)
    .setExpiration(now + sessionMinutes * 60s)   â† from SystemSetting DB
    .signWith(HS512, SECRET_KEY)
    .compact()

Validate (JwtAuthenticationFilter):
  1. Extract "Authorization: Bearer <token>" header
  2. jwtUtil.extractUsername(token) â†’ email
  3. Load User from DB by email
  4. jwtUtil.validateToken(token, email) â†’ checks sig + expiry
  5. jwtUtil.extractTokenVersion(token) == user.tokenVersion â†’ anti-replay
  6. Inject ROLE_<ROLE> into SecurityContext
```

### 8.4 Exam Eligibility Algorithm (`StudentExamEligibilityService.java`)
```
Input: User (student)

Gate: feesPaid && isEligible && examAccessAllowed  â†’ else EligibilityException

backlogs = StudentBacklogRepo.findByStudentIdAndCleared(studentId, false)
hasActiveBacklogs = backlogs.size() > 0 || studentType == "BACKLOG"

IF hasActiveBacklogs:
    subjectIds = backlogs.stream().map(StudentBacklog::getSubjectId).filter(notNull)
    return ExamRepo.findBacklogExams(subjectIds, BACKLOG, "upcoming")
           .filter(exam.subjectId != null)

ELSE IF student has college + course + semester:
    sem = Integer.parseInt(student.getSemester())
    subjects = SubjectRepo.findByCourseSemesterAndCollege(course, sem, collegeId)
    subjectIds = subjects.stream().map(Subject::getId)
    return ExamRepo.findRegularExamsBySubjectIds(subjectIds, REGULAR, "upcoming")
           .filter(exam.subjectId != null)

ELSE (incomplete profile fallback):
    return ExamRepo.findRegularExams(semester, REGULAR, "upcoming")
```

### 8.5 QR Token Generation & Verification Algorithm
```
GENERATE:
  raw = studentId + "|" + examId + "|" + System.currentTimeMillis() + "|" + SECRET
  hash = SHA-256(raw)                  // 64-char hex
  store QrCode(studentId, examId, hash, now, now+10min, used=false)
  return hash                          // embedded in QR image

VERIFY (POST /api/qr/verify):
  token = request body
  qr = qrRepo.findByHashedToken(token)
  if qr empty           â†’ 401 "Token not found"
  if qr.isUsed()        â†’ 401 "Token already used"   â† single-use guarantee
  if qr.expiresAt < now â†’ 401 "Token expired"         â† time-bound guarantee
  qr.setUsed(true) â†’ save                             â† atomic invalidation
  user = userRepo.findById(qr.studentId)
  biometricRecentlyVerified = biometricEnrolled
      && biometricLastVerified != null
      && biometricLastVerified >= now - 5min
  return QrVerificationResponse(student, exam, biometric status, institution)
```

### 8.6 Biometric Verification Algorithm (`BiometricController.java`)
```
Input: { studentId, fingerprint, examId }  (JWT: SUPERVISOR role required)

templateHash = SHA-256(fingerprint)
user = userRepo.findById(studentId)
existing = user.biometricTemplateHash

IF existing == null:              // Enrollment (first scan)
    user.biometricTemplateHash = templateHash
    user.biometricEnrolled = true
    user.biometricEnrolledAt = now
    user.biometricLastVerified = now
    user.biometricVerified = true
    attendance(PRESENT, SUCCESS)
    return {success:true, score:100.0}

ELSE IF templateHash == existing:  // Match
    user.biometricLastVerified = now
    user.biometricVerified = true
    attendance(PRESENT, SUCCESS)
    return {success:true, score:98.5}

ELSE:                              // Mismatch
    user.biometricVerified = false
    attendance(ABSENT, FAILED)
    return HTTP 401

attendance() = ExamAttendanceRepo.findByStudentIdAndExamId()
    â†’ if exists: skip (idempotent, unique constraint)
    â†’ else: save new ExamAttendance record
```

### 8.7 Supervisor Access Algorithm
```
HEAD Supervisor:
  exams = examRepo.findAll()
           .filter(exam.collegeId == supervisor.collegeId)
  // Sees ALL exams in their college

EXAM Supervisor:
  exams = examRepo.findAll()
           .filter(exam.supervisorIds.contains(supervisor.userId))
  // Sees ONLY explicitly assigned exams
```

### 8.8 OTP Generation & Verification
```
Generate:
  otp = String.valueOf(100000 + Random.nextInt(900000))  // 6-digit
  expiry = now + 5 minutes
  delete old OTPs for same email/phone (prevent replay)
  save OtpEntity(email/phone, otp, expiry, verified=false)
  send via JavaMailSender (email) or Fast2SMS REST API (phone)

Verify:
  entities = otpRepo.findByEmail(email)
  validOtp = entities.stream()
      .filter(e -> e.otp.equals(input) && e.expiryTime.isAfter(now))
      .findFirst()
  if present: e.verified=true, save â†’ return true
  else: return false
```

---

## 9. SYSTEM FLOW DIAGRAMS

### 9.1 Complete Authentication Flow
```
User opens index.html (port 8080)
    â”‚
    â”œâ”€ Enter email + password
    â”œâ”€ POST /api/auth/login
    â”‚   â”œâ”€ Load User by email
    â”‚   â”œâ”€ BCrypt.matches(input, stored)
    â”‚   â”œâ”€ Generate JWT(email, tokenVersion)
    â”‚   â””â”€ Return {token, role, userId}
    â”‚
    â”œâ”€ Browser stores JWT in localStorage
    â”‚
    â””â”€ Redirect by role:
        â”œâ”€ STUDENT        â†’ student_dashboard.html
        â”œâ”€ SUPERVISOR     â†’ supervisor_dashboard.html
        â”œâ”€ UNIVERSITY_ADMIN â†’ university_dashboard.html
        â””â”€ SUPERADMIN     â†’ super_admin_dashboard.html
```

### 9.2 Student Exam Registration & Hall Ticket Flow
```
Student Dashboard loads
    â”‚
    â”œâ”€ GET /api/student/eligible-exams (JWT)
    â”‚   â””â”€ EligibilityService runs gate checks + backlog/regular logic
    â”‚
    â”œâ”€ Student selects exam â†’ POST /api/student/register
    â”‚   â””â”€ Creates ExamRegistration(status=PENDING)
    â”‚
    â”œâ”€ Admin approves â†’ PATCH registration status = APPROVED
    â”‚
    â”œâ”€ Admin triggers hall ticket release
    â”‚   â””â”€ POST /api/university/release-hallticket/{examId}
    â”‚       â””â”€ Sets hallTicketReleased=true for all APPROVED registrations
    â”‚
    â”œâ”€ Student clicks "Download Hall Ticket"
    â”‚   â”œâ”€ POST /api/qr/generate â†’ returns SHA-256 token
    â”‚   â””â”€ Frontend generates QR image from token
    â”‚       â””â”€ Renders hall ticket HTML with student info + QR
```

### 9.3 Supervisor Exam Hall Verification Flow
```
Supervisor opens supervisor_dashboard.html
    â”‚
    â”œâ”€ Login â†’ JWT issued (ROLE_SUPERVISOR)
    â”‚
    â”œâ”€ HEAD: sees all college exams
â”‚   EXAM: sees only assigned exams
    â”‚
    â”œâ”€ Opens student verification panel
    â”‚
    â”œâ”€ SCAN QR:
    â”‚   â”œâ”€ POST /api/qr/verify { token }
    â”‚   â””â”€ Response: student name, PRN, photo, seat, hall, biometric status
    â”‚
    â””â”€ BIOMETRIC:
        â”œâ”€ POST /api/biometric/capture â†’ simulates device capture
        â”œâ”€ POST /api/biometric/verify { studentId, fingerprint, examId }
        â””â”€ Response: MATCH/MISMATCH + attendance recorded
```

### 9.4 Admin Exam Creation Flow
```
Admin opens university_dashboard.html â†’ Manage Exams
    â”‚
    â”œâ”€ Select College â†’ dynamically loads Departments
    â”œâ”€ Select Department â†’ loads Courses
    â”œâ”€ Select Course + Semester â†’ loads Subjects
    â”œâ”€ Select Exam Type (REGULAR/BACKLOG)
    â”œâ”€ Select multiple subjects (List<Long> subjectIds)
    â”œâ”€ Set Date, Time, Mode, Location
    â”œâ”€ Assign supervisors (select from list of SUPERVISOR users)
    â”‚
    â””â”€ POST /api/admin/university-exam (or /api/university/{id}/exam)
        â””â”€ Saves UniversityExam with embedded schedule, supervisorIds, subjectIds
```

---

## 10. FRONTEND STRUCTURE

### 10.1 HTML Pages

| File | Role | Purpose |
|---|---|---|
| `index.html` | All | Landing + login portal |
| `super_admin_dashboard.html` | SuperAdmin | System-wide control, reports, institution management |
| `university_dashboard.html` | Admin | College management, exam scheduling, student management |
| `supervisor_dashboard.html` | Supervisor | QR scanning, biometric verification, attendance |
| `student_dashboard.html` | Student | Exam registration, hall ticket, backlog view |
| `admin_dashboard.html` | Admin | Alternate admin view |
| `auth_portal.html` | All | OTP-based authentication portal |
| `institution_onboarding.html` | SuperAdmin | New institution registration |
| `online_exam_portal.html` | Student | Online exam interface |
| `student_admit_card.html` | Student | Hall ticket/admit card display with QR |

### 10.2 JavaScript Modules

| File | Purpose |
|---|---|
| `js/main.js` | Global auth, JWT storage, logout, common fetch wrapper |
| `js/student_dashboard.js` | Backlog table, profile load, eligible exams |
| `js/university/CreateExam.js` | Dynamic dropdowns (collegeâ†’deptâ†’courseâ†’semesterâ†’subjects), multi-subject selection, exam POST |
| `js/pages/students.js` | Student list, CSV upload, search/filter by college |
| `js/pages/staff.js` | Supervisor list, creation, assignment |
| `js/utils/` | Shared utility functions |
| `js/components/` | Reusable UI components |
| `script.js` | Root-level utility script |

### 10.3 API Integration Pattern
All JS modules use `fetch()` with the pattern:
```javascript
const response = await fetch('/api/endpoint', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + localStorage.getItem('token')
    },
    body: JSON.stringify(payload)
});
const data = await response.json();
```
JWT is stored in `localStorage` after login and attached to every protected request.

---

## 11. BACKEND STRUCTURE

### 11.1 Controllers (25 total)

| Controller | Route Prefix | Responsibility |
|---|---|---|
| `AuthController` | `/api/auth` | Login, register, password change |
| `UniversityController` | `/api/university` | Exam CRUD, student/staff list, hall ticket release |
| `AdminController` | `/api/admin` | Full admin operations, seat allocation |
| `AdminStudentController` | `/api/admin/students` | Student CRUD, CSV import |
| `AdminSupervisorController` | `/api/admin/supervisors` | Supervisor creation |
| `AdminCollegeController` | `/api/admin/colleges` | College CRUD |
| `AdminDepartmentController` | `/api/admin/departments` | Department CRUD |
| `AdminSubjectController` | `/api/admin/subjects` | Subject CRUD, CSV import |
| `AdminBacklogController` | `/api/admin/backlogs` | Backlog upload, view |
| `BiometricController` | `/api/biometric` | Fingerprint capture, verify |
| `QrController` | `/api/qr` | QR generate, verify |
| `OtpController` | `/api/otp` | Send/verify email+phone OTP |
| `SupervisorController` | `/api/supervisor` | Supervisor dashboard data |
| `StudentController` | `/api/student` | Student registration, profile |
| `StudentExamController` | `/api/student/exam` | Student exam actions |
| `ProfileController` | `/api/profile` | Profile update, document upload |
| `InstitutionController` | `/api/institution` | Institution management |
| `CommunicationController` | `/api/communication` | Internal messaging |
| `AdminNotificationController` | `/api/admin/notifications` | Alerts & notifications |
| `SystemSettingsController` | `/api/settings` | Key-value system settings |
| `FraudController` | `/api/fraud` | Fraud log access |
| `HomeController` | `/` | Serves index.html |
| `DebugRegistrationController` | `/api/debug` | Dev-only debug endpoints |
| `TestController` | `/api/test` | Public health check |

### 11.2 Services (13 total)

| Service | Responsibility |
|---|---|
| `AuthService` | Login logic, BCrypt comparison |
| `UserManagementService` | User creation, update, CSV parsing |
| `StudentExamEligibilityService` | Eligibility algorithm |
| `QrService` | SHA-256 token generation + verification |
| `OtpService` | Email (SMTP) + SMS (Fast2SMS) OTP |
| `PdfService` | OpenPDF report generation |
| `ExamService` | Exam CRUD operations |
| `AttendanceService` | Attendance record management |
| `AiVerificationClientService` | AI verification client |
| `AlertNotificationService` | Alert creation/dispatch |
| `CsvProcessingService` | Generic CSV parsing utility |
| `SettingsService` | SystemSetting DB read/write |

### 11.3 Exception Handling
- `EligibilityException` â€” thrown by eligibility service, caught by controller and returned as HTTP 403
- Global `@ControllerAdvice` catches unhandled exceptions and returns structured JSON error responses

---

## 12. FEATURES & HIGHLIGHTS

1. **Multi-College Isolation** â€” `collegeId` on users, exams, subjects enforces strict data partitioning
2. **JWT + Token Versioning** â€” incrementing `tokenVersion` on profile change invalidates all old sessions globally
3. **SHA-256 QR Authentication** â€” single-use, time-expiring tokens with cryptographic hash
4. **SHA-256 Biometric Hashing** â€” fingerprint template stored as irreversible hash; hash comparison for match
5. **AES-256-GCM Encryption Utility** â€” available for sensitive payload encryption with authenticated tag
6. **BCrypt Password Storage** â€” industry-standard password hashing with salt
7. **Dual OTP Channels** â€” Gmail SMTP for email OTP, Fast2SMS REST API for phone OTP
8. **Backlog-Aware Eligibility** â€” automatic subject-intersection logic between StudentBacklog and exam subjects
9. **Multi-Subject Exams** â€” `List<Long> subjectIds` via `@ElementCollection`
10. **Multi-Supervisor Assignment** â€” `List<Long> supervisorIds` per exam
11. **HEAD vs EXAM Supervisor** â€” different data scope based on `supervisorType`
12. **PRN-Sorted Seat Allocation** â€” deterministic, reproducible seating
13. **Dual Exam Model** â€” Legacy `Exam` + `UniversityExam` unified via `ExamResponseDTO`
14. **OpenPDF Reports** â€” System, Security, Performance, Rankings reports as downloadable PDFs
15. **Exam Attendance Audit** â€” `exam_attendance` table with biometric result + supervisor ID per entry
16. **Single-Port Deployment** â€” frontend + backend on port 8080; no CORS needed in production

---

## 13. ADVANTAGES

1. **Tamper-Proof Hall Tickets** â€” SHA-256 tokens cannot be reverse-engineered; single-use prevents sharing
2. **Automated Eligibility** â€” Zero manual checking; backlog-subject intersection runs in milliseconds
3. **Cryptographic Identity** â€” Biometric data stored as irreversible hash; no raw biometric stored in DB
4. **Audit Trail** â€” `exam_attendance`, `supervisor_action_logs`, `fraud_logs` provide complete accountability
5. **Scalable Architecture** â€” Multi-college data isolation allows adding new colleges without code changes
6. **OTP-Secured Login** â€” Two-factor authentication via email and/or phone OTP
7. **Session Control** â€” `tokenVersion` allows instant revocation of all sessions for any user
8. **Configurable Sessions** â€” `SESSION_TIMEOUT_MINUTES` stored in DB, changeable without redeployment

---

## 14. LIMITATIONS

1. **Biometric Hardware** â€” Current implementation uses software-submitted fingerprint strings; real hardware SDK integration (e.g., Mantra, Morpho) not yet connected
2. **Single-Use QR Window** â€” 10-minute QR expiry may cause issues if student's device clock is skewed
3. **Monolithic Deployment** â€” A single Spring Boot instance handles all traffic; no horizontal scaling
4. **SHA-256 for Biometrics** â€” Hash comparison requires exact string match; real biometric systems use fuzzy matching algorithms (minutiae matching) for tolerance

---

## 15. FUTURE SCOPE

1. **Hardware Biometric SDK** â€” Integrate Mantra RD Service or Morpho SDK for actual fingerprint device capture replacing software simulation
2. **Fuzzy Biometric Matching** â€” Replace exact SHA-256 hash compare with a minutiae-based matching algorithm with tolerance threshold
3. **WebSocket Attendance** â€” Real-time attendance dashboard for admins using Spring WebSocket
4. **Mobile Supervisor App** â€” Native Android/iOS app for faster QR scanning using device camera
5. **Microservices Migration** â€” Split into Exam Service, Auth Service, Notification Service, Report Service for independent scaling
6. **Face Recognition** â€” Add OpenCV / AWS Rekognition face match as a second biometric factor
7. **Blockchain Hall Tickets** â€” Issue hall tickets as NFT/blockchain records for absolute tamper-proof verification
