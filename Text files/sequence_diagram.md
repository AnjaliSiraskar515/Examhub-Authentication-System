# Exam Authentication System: Detailed Sequence Diagrams

These sequence diagrams illustrate the core workflows of the Exam Authentication System. They are designed to be included in your project report to demonstrate the interaction between various system components, actors, and databases.

## 1. System Setup & User Onboarding Flow

This sequence outlines the process of a Super Admin onboarding an institution, followed by the University Admin enrolling students and setting up credentials.

```mermaid
sequenceDiagram
    autonumber
    actor SA as Super Admin
    actor UA as University Admin
    participant SYS as System (Spring Boot)
    participant DB as MySQL Database
    participant MAIL as Email/SMS Service
    actor STUD as Student

    %% Super Admin Action
    SA->>SYS: POST /api/admin/institution/register (Details)
    activate SYS
    SYS->>DB: Save Institution & generate API Keys
    SYS->>DB: Create Root University Admin User
    SYS-->>SA: 200 OK (Institution Created)
    deactivate SYS

    %% University Admin Action (Student Enrollment)
    UA->>SYS: POST /api/auth/login (UA Credentials)
    SYS-->>UA: Return JWT (ROLE_UNIVERSITY_ADMIN)
    
    UA->>SYS: POST /api/admin/students/upload (CSV File)
    activate SYS
    SYS->>SYS: Parse CSV & validate data
    
    loop For each student in CSV
        SYS->>SYS: Extract Details & generate default password (PRN)
        SYS->>SYS: Hash password (BCrypt)
        SYS->>DB: Save User Entity (ROLE_STUDENT)
        SYS->>MAIL: Trigger Welcome Email with Credentials
        MAIL-->>STUD: Deliver Email (Login ID & Default Password)
    end
    
    SYS-->>UA: 200 OK (Bulk Upload Successful)
    deactivate SYS
```

## 2. Exam Creation & Pre-Exam Setup Flow

This sequence details how a University Admin creates an exam, assigns supervisors, and allocates seats.

```mermaid
sequenceDiagram
    autonumber
    actor UA as University Admin
    participant SYS as System (Spring Boot)
    participant DB as MySQL Database
    actor HEAD as Head Supervisor
    actor EXAM_SUP as Exam Supervisor

    %% Exam Creation
    UA->>SYS: GET /api/admin/subjects (Filter by Course/Sem/College)
    SYS->>DB: Fetch Subjects
    DB-->>SYS: Subject List
    SYS-->>UA: Return Subjects

    UA->>SYS: POST /api/university/exams (Exam Details, Subject IDs, Supervisors)
    activate SYS
    SYS->>DB: Save Exam Entity & Join Tables (Supervisors/Subjects)
    SYS-->>UA: 201 Created
    deactivate SYS

    %% Seat Allocation
    UA->>SYS: POST /api/admin/exams/{examId}/allocate-seats
    activate SYS
    SYS->>DB: Fetch registered students (Sorted by PRN)
    SYS->>DB: Fetch Exam Halls
    SYS->>SYS: Execute sequential seat allocation algorithm
    SYS->>DB: Save SeatAllocation entities
    SYS-->>UA: 200 OK (Seats Allocated)
    deactivate SYS

    %% Supervisor Access Verification
    HEAD->>SYS: GET /api/supervisor/exams
    SYS->>DB: Fetch exams where exam.collegeId == supervisor.collegeId
    DB-->>SYS: Exams List
    SYS-->>HEAD: View all college exams

    EXAM_SUP->>SYS: GET /api/supervisor/exams
    SYS->>DB: Fetch exams where exam.supervisorIds contains supervisor.userId
    DB-->>SYS: Exams List
    SYS-->>EXAM_SUP: View assigned exams only
```

## 3. Student Registration & Hall Ticket Flow

This diagram illustrates a student's journey from logging in (including the mandatory first-time password change) to registering for an exam and downloading a cryptographically secure hall ticket.

```mermaid
sequenceDiagram
    autonumber
    actor STUD as Student
    participant SYS as System (Spring Boot)
    participant DB as MySQL Database
    actor UA as University Admin

    %% First Login
    STUD->>SYS: POST /api/auth/login (Email, Default PRN Password)
    SYS->>DB: Verify Credentials
    SYS-->>STUD: Return JWT + firstLogin flag = true
    
    STUD->>SYS: POST /api/student/change-password (New Password)
    SYS->>DB: Update BCrypt Hash & Set firstLogin = false, Increment tokenVersion
    SYS-->>STUD: Password Changed Successfully

    %% Exam Eligibility & Registration
    STUD->>SYS: GET /api/student/eligible-exams
    activate SYS
    SYS->>DB: Check eligibility gates (feesPaid, examAccessAllowed)
    SYS->>DB: Fetch Backlog / Regular Subjects
    SYS->>DB: Query Upcoming Exams
    DB-->>SYS: Eligible Exams List
    SYS-->>STUD: Return Eligible Exams
    deactivate SYS

    STUD->>SYS: POST /api/student/register (Exam ID)
    SYS->>DB: Create ExamRegistration (PENDING)
    SYS-->>STUD: Registration Submitted

    %% Admin Approval
    UA->>SYS: PATCH /api/university/registrations/{id}/approve
    SYS->>DB: Update status to APPROVED
    
    UA->>SYS: POST /api/university/release-hallticket/{examId}
    SYS->>DB: Set hallTicketReleased = true for APPROVED
    SYS-->>UA: Hall Tickets Released

    %% Hall Ticket Generation
    STUD->>SYS: POST /api/qr/generate
    activate SYS
    SYS->>SYS: Compose payload (studentId | examId | timestamp | SECRET)
    SYS->>SYS: SHA-256 Hash payload
    SYS->>DB: Store QrCode(hashedToken, expiresAt)
    SYS-->>STUD: Return hashedToken (QR Image rendered on Frontend)
    deactivate SYS
```

## 4. Exam Day Verification Flow (QR + Biometric)

This represents the high-security verification process at the exam hall entrance, involving QR scanning and biometric fingerprint matching.

```mermaid
sequenceDiagram
    autonumber
    actor STUD as Student
    actor EXAM_SUP as Exam Supervisor
    participant SYS as System (Spring Boot)
    participant DB as MySQL Database

    %% QR Verification
    STUD->>EXAM_SUP: Present Hall Ticket QR Code
    EXAM_SUP->>SYS: POST /api/qr/verify (Scanned Token)
    activate SYS
    SYS->>DB: Lookup qr_tokens by hashedToken
    alt Token Invalid / Used / Expired
        SYS-->>EXAM_SUP: 401 Unauthorized (Reject)
    else Token Valid
        SYS->>DB: Set qr.used = true
        SYS->>DB: Fetch User details (Mask Aadhar)
        SYS-->>EXAM_SUP: Return Student Details + Biometric Status
    end
    deactivate SYS

    %% Biometric Verification
    STUD->>EXAM_SUP: Provide Fingerprint Scan
    EXAM_SUP->>SYS: POST /api/biometric/verify (Fingerprint Data, StudentId, ExamId)
    activate SYS
    SYS->>SYS: Generate templateHash = SHA-256(fingerprint)
    SYS->>DB: Fetch user.biometricTemplateHash
    
    alt First Time (Enrollment)
        SYS->>DB: Save templateHash, set biometricEnrolled = true
        SYS->>DB: Save ExamAttendance (PRESENT, SUCCESS)
        SYS-->>EXAM_SUP: 200 OK (Enrolled & Verified)
    else Subsequent Scans (Match)
        SYS->>SYS: Compare templateHash with DB hash
        alt Hashes Match
            SYS->>DB: Save ExamAttendance (PRESENT, SUCCESS)
            SYS-->>EXAM_SUP: 200 OK (Match Confirmed)
        else Hashes Mismatch
            SYS->>DB: Save ExamAttendance (ABSENT, FAILED)
            SYS-->>EXAM_SUP: 401 Unauthorized (Biometric Mismatch)
        end
    end
    deactivate SYS
```
