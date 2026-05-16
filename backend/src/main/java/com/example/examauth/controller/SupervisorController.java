package com.example.examauth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;
import com.example.examauth.model.User;

@RestController
@RequestMapping("/api/supervisor")
@CrossOrigin // Allow frontend access
public class SupervisorController {

    private final com.example.examauth.repo.UserRepository userRepository;
    private final com.example.examauth.repo.QrRepository qrRepository;
    private final com.example.examauth.repo.ExamRepository examRepository;
    private final com.example.examauth.student_exam.university.repo.UniversityExamRepository universityExamRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public SupervisorController(com.example.examauth.repo.UserRepository userRepository,
            com.example.examauth.repo.QrRepository qrRepository,
            com.example.examauth.repo.ExamRepository examRepository,
            com.example.examauth.student_exam.university.repo.UniversityExamRepository universityExamRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.qrRepository = qrRepository;
        this.examRepository = examRepository;
        this.universityExamRepository = universityExamRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @org.springframework.beans.factory.annotation.Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
            return ResponseEntity.status(401).body(Map.<String, Object>of("error", "Unauthorized"));
        }
        String email = authentication.getName();
        User user = userRepository.findFirstByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.<String, Object>of("error", "User not found"));
        }

        // Fetch real counts (dashboard stats)
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();

        // Use the deterministic exam list to count proper assigned exams and students
        List<Map<String, Object>> myExams = getExams(authentication);

        // Collect students for every assigned exam using getStudents(examId) — same method
        // that powers the live monitoring table, so no college-filter mismatch.
        List<Map<String, Object>> myStudents = new ArrayList<>();
        for (Map<String, Object> exam : myExams) {
            Object idObj = exam.get("id");
            if (idObj != null) {
                Long eid = ((Number) idObj).longValue();
                myStudents.addAll(getStudents(eid));
            }
        }

        long totalStudents = myStudents.size();
        long fullyVerifiedCount = myStudents.stream()
                .filter(s -> Boolean.TRUE.equals(s.get("biometricVerified")))
                .count();
        long pendingCount = Math.max(0, totalStudents - fullyVerifiedCount);
        long totalScans = fullyVerifiedCount; // QR Scans equals verified students for today

        Map<String, Object> profile = new HashMap<>();
        profile.put("scansToday", totalScans);
        profile.put("pendingVerifications", pendingCount);
        profile.put("studentsCount", totalStudents);
        profile.put("verifiedStudentsCount", fullyVerifiedCount);
        profile.put("assignedExamsCount", (long) myExams.size());

        // Logged-in supervisor's identity & affiliation only
        profile.put("name", user.getName() != null ? user.getName() : "");
        profile.put("email", user.getEmail() != null ? user.getEmail() : "");
        profile.put("phone", user.getPhoneNumber());
        // University: prefer user.universityName; fall back to college.universityName
        String resolvedUniversity = user.getUniversityName();
        if ((resolvedUniversity == null || resolvedUniversity.isBlank()) && user.getCollege() != null) {
            resolvedUniversity = user.getCollege().getUniversityName();
        }
        profile.put("university", resolvedUniversity != null ? resolvedUniversity : "");
        profile.put("college", (user.getCollege() != null && user.getCollege().getName() != null)
                ? user.getCollege().getName()
                : user.getCollegeName());
        profile.put("collegeId", user.getCollege() != null ? user.getCollege().getId() : null);
        profile.put("supervisorType", user.getSupervisorType() != null ? user.getSupervisorType() : "EXAM");
        profile.put("department", user.getDepartment());
        profile.put("employeeId", user.getEmployeeId());
        profile.put("avatar", user.getPhotoPath());
        profile.put("biometricEnrolled", user.isBiometricEnrolled());

        List<Map<String, String>> docs = new ArrayList<>();
        if (user.getIdProofPath() != null)
            docs.add(Map.of("filename", "ID Proof", "status", "Uploaded"));
        if (docs.isEmpty())
            docs.add(Map.of("filename", "Supervisor_Guidelines.pdf", "status", "Pending"));

        profile.put("documents", docs);
        profile.put("firstLogin", user.getFirstLogin() != null ? user.getFirstLogin() : false);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/exams")
    public List<Map<String, Object>> getExams(Authentication authentication) {
        String supervisorName = "";
        Long supervisorId = 0L;
        String supervisorType = "EXAM";
        Long collegeId = null;
        String universityName = null;
        if (authentication != null && authentication.getName() != null) {
            String email = authentication.getName();
            User user = userRepository.findFirstByEmail(email).orElse(null);
            if (user != null) {
                supervisorName = user.getName() != null ? user.getName() : "";
                supervisorId = user.getUserId();
                supervisorType = user.getSupervisorType() != null ? user.getSupervisorType() : "EXAM";
                if (user.getCollege() != null) {
                    collegeId = user.getCollege().getId();
                    if (user.getCollege().getUniversityName() != null) {
                        universityName = user.getCollege().getUniversityName();
                    }
                }
            }
        }
        final String capturedUniName = universityName;
        String instCode = null;
        if (capturedUniName != null) {
            instCode = userRepository.findByRole("UNIVERSITY_ADMIN").stream()
                .filter(u -> capturedUniName.equalsIgnoreCase(u.getUniversityName()))
                .map(User::getInstitutionCode)
                .findFirst().orElse(null);
        }
        
        final Long sId = supervisorId;
        final String sType = supervisorType;
        final Long cId = collegeId;
        final String uniName = universityName;
        final String finalInstCode = instCode;

        List<Map<String, Object>> result = new ArrayList<>();

        // 1. Old Exam table
        examRepository.findAll().stream()
                .filter(exam -> {
                    if ("HEAD".equalsIgnoreCase(sType) && cId != null) {
                        return cId.equals(exam.getCollegeId()) ||
                                (exam.getSupervisorId() != null && exam.getSupervisorId().equals(sId)) ||
                                (exam.getSupervisorIds() != null && exam.getSupervisorIds().contains(sId));
                    } else {
                        return (exam.getSupervisorId() != null && exam.getSupervisorId().equals(sId)) ||
                                (exam.getSupervisorIds() != null && exam.getSupervisorIds().contains(sId));
                    }
                })
                .forEach(exam -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", exam.getExamId());
                    map.put("title", exam.getExamName());
                    map.put("date", exam.getDate() != null ? exam.getDate().toString() : "TBD");
                    map.put("duration", exam.getDurationMinutes());
                    map.put("startTime", exam.getStartTime() != null ? exam.getStartTime().toString() : "TBD");
                    map.put("mode", exam.getMode());
                    map.put("status", exam.getStatus());
                    map.put("location", exam.getLocation());
                    result.add(map);
                });

        // 2. New UniversityExam table
        universityExamRepository.findAll().stream()
                .filter(uExam -> {
                    if ("HEAD".equalsIgnoreCase(sType)) {
                        boolean isCollegeExam = cId != null && cId.equals(uExam.getCollegeId());
                        boolean isUniversityExam = finalInstCode != null && finalInstCode.equalsIgnoreCase(uExam.getInstitutionCode());
                        return isCollegeExam || isUniversityExam ||
                                (uExam.getSupervisorId() != null && uExam.getSupervisorId().equals(sId)) ||
                                (uExam.getSupervisorIds() != null && uExam.getSupervisorIds().contains(sId));
                    } else {
                        return (uExam.getSupervisorId() != null && uExam.getSupervisorId().equals(sId)) ||
                                (uExam.getSupervisorIds() != null && uExam.getSupervisorIds().contains(sId));
                    }
                })
                .forEach(uExam -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", uExam.getId());
                    map.put("title", uExam.getSessionName());
                    map.put("date", uExam.getExamDate() != null ? uExam.getExamDate().toString() : "TBD");
                    map.put("duration",
                            uExam.getSchedule() != null && uExam.getSchedule().getStartTime() != null &&
                                    uExam.getSchedule().getEndTime() != null
                                            ? java.time.Duration.between(
                                                    uExam.getSchedule().getStartTime(),
                                                    uExam.getSchedule().getEndTime()).toMinutes()
                                            : 0);
                    map.put("startTime",
                            uExam.getSchedule() != null && uExam.getSchedule().getStartTime() != null
                                    ? uExam.getSchedule().getStartTime().toString()
                                    : "TBD");
                    map.put("mode", uExam.getMode());
                    map.put("status", uExam.getStatus());
                    map.put("location", uExam.getCenterName() != null ? uExam.getCenterName() : "");
                    result.add(map);
                });

        return result;
    }

    @GetMapping("/students")
    public List<Map<String, Object>> getStudents(@RequestParam(value = "examId", required = false) Long examId) {

        // If examId is provided, return ONLY students actually registered for that exam
        if (examId != null) {
            List<com.example.examauth.student_exam.model.ExamRegistration> registrations = examRegistrationRepository
                    .findByExamId(examId);

            return registrations.stream().map(reg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", reg.getStudentId());
                map.put("regno", reg.getPrn() != null ? reg.getPrn() : "EXH-2025-" + reg.getStudentId());
                map.put("course", reg.getCourse());
                map.put("examName", reg.getExamSession());
                map.put("status", reg.getRegistrationStatus().name());

                // Enrich with live user data (name, email, verification)
                User student = userRepository.findById(reg.getStudentId()).orElse(null);
                if (student != null) {
                    map.put("name", student.getName() != null ? student.getName() : reg.getFullName());
                    map.put("email", student.getEmail());
                    map.put("qrVerified", student.getQrVerified());
                    map.put("biometricVerified", student.getBiometricVerified());
                } else {
                    map.put("name", reg.getFullName() != null ? reg.getFullName() : "Student " + reg.getStudentId());
                    map.put("email", "");
                    map.put("qrVerified", false);
                    map.put("biometricVerified", false);
                }

                // Live monitoring simulation for non-ended exams
                map.put("warningCount", (int) (Math.random() * 3));
                map.put("lastActivityTime",
                        java.time.LocalTime.now().minusSeconds((long) (Math.random() * 300)).toString());
                String[] statuses = { "ACTIVE", "IDLE", "DISCONNECTED", "SUSPICIOUS" };
                map.put("liveStatus", statuses[(int) (Math.random() * statuses.length)]);

                return map;
            }).collect(java.util.stream.Collectors.toList());
        }

        // No examId — return empty list (don't show all students by default)
        return new ArrayList<>();
    }

    // New Repositories (Ideally inject via constructor)
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.examauth.repo.IncidentReportRepository incidentReportRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.examauth.repo.SupervisorActionLogRepository supervisorActionLogRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.examauth.repo.AlertRepository alertRepository;

    // --- Phase 1: Exam Control Endpoints ---

    @PutMapping("/exam/{id}/status")
    public Map<String, Object> updateExamStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        
        Optional<com.example.examauth.model.Exam> examOpt = examRepository.findById(id);
        if (examOpt.isPresent()) {
            com.example.examauth.model.Exam exam = examOpt.get();
            exam.setStatus(status);
            examRepository.save(exam);
        } else {
            Optional<com.example.examauth.student_exam.university.model.UniversityExam> uExamOpt = universityExamRepository.findById(id);
            if (uExamOpt.isPresent()) {
                com.example.examauth.student_exam.university.model.UniversityExam uExam = uExamOpt.get();
                uExam.setStatus(status);
                universityExamRepository.save(uExam);
            } else {
                throw new RuntimeException("Exam not found with id: " + id);
            }
        }

        // Audit Log
        com.example.examauth.model.SupervisorActionLog log = new com.example.examauth.model.SupervisorActionLog();
        log.setAction("STATUS_UPDATE");
        log.setExamId(id);
        log.setDetails("Changed status to " + status);
        log.setSupervisorId(1L); // Mock Supervisor ID
        supervisorActionLogRepository.save(log);

        return Map.of("success", true, "message", "Exam status updated to " + status);
    }

    // --- Phase 1: Incident Reporting ---

    @PostMapping("/incident")
    public Map<String, Object> reportIncident(@RequestBody com.example.examauth.model.IncidentReport report) {
        incidentReportRepository.save(report);

        // Create Alert
        com.example.examauth.model.Alert alert = new com.example.examauth.model.Alert(
                "MALPRACTICE",
                "Incident reported: " + report.getReason(),
                report.getExamId());
        alertRepository.save(alert);

        return Map.of("success", true, "message", "Incident reported successfully");
    }

    @GetMapping("/exam/{id}/alerts")
    public List<com.example.examauth.model.Alert> getExamAlerts(@PathVariable("id") Long id) {
        return alertRepository.findByExamIdOrderByTimestampDesc(id);
    }

    // --- Phase 1: Broadcast ---

    @PostMapping("/broadcast")
    public org.springframework.http.ResponseEntity<?> broadcastMessage(@RequestBody Map<String, Object> payload) {
        String message = (String) payload.get("message");
        Long examId = ((Number) payload.get("examId")).longValue();

        // Log action
        com.example.examauth.model.SupervisorActionLog log = new com.example.examauth.model.SupervisorActionLog();
        log.setSupervisorId(1L); // Mock ID
        log.setAction("BROADCAST: " + message);
        log.setExamId(examId);
        log.setTimestamp(java.time.LocalDateTime.now());
        supervisorActionLogRepository.save(log);

        return org.springframework.http.ResponseEntity.ok(Map.of("success", true, "message", "Broadcast sent"));
    }

    @GetMapping("/exam/{id}/summary")
    public org.springframework.http.ResponseEntity<?> getExamSummary(@PathVariable("id") Long id) {
        // 1. Get Exam Details to filter students (Reusing logic from getStudents)
        com.example.examauth.model.Exam exam = examRepository.findById(id).orElse(null);
        String targetExamName = (exam != null) ? exam.getExamName() : "";

        // 2. Filter Students Assigned to this Exam
        List<User> students = userRepository.findByRole("STUDENT").stream()
                .filter(user -> {
                    String assignedExam = "Advanced Java Programming";
                    if (user.getUsername() != null) {
                        String lowerUser = user.getUsername().trim().toLowerCase();
                        if (lowerUser.startsWith("student")) {
                            assignedExam = "Theory of Computation";
                        } else if (lowerUser.startsWith("os_student") || lowerUser.startsWith("os")) {
                            assignedExam = "Operating System";
                        }
                    }
                    return targetExamName.equals(assignedExam);
                })
                .collect(java.util.stream.Collectors.toList());

        long totalStudents = students.size();

        // 3. Real Incident Count
        long incidentsCount = incidentReportRepository.countByExamId(id);

        // 4. Calculate Stats (Deterministic Match with getStudents)
        long submittedCount = 0;
        long verifiedCount = 0;

        for (User user : students) {
            boolean isVerified = Boolean.TRUE.equals(user.getBiometricVerified());
            if (isVerified) {
                verifiedCount++;
                submittedCount++; // Verified always = SUBMITTED
            } else {
                // Same deterministic logic as getStudents
                int hash = (user.getUsername() != null) ? user.getUsername().hashCode() : 0;
                if (Math.abs(hash) % 10 < 8) {
                    submittedCount++;
                }
            }
        }

        long notSubmittedCount = students.size() - submittedCount;
        long disconnectedCount = 0; // In ended state, we don't track live disconnects usually

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalStudents", (long) students.size());
        summary.put("submitted", submittedCount);
        summary.put("notSubmitted", notSubmittedCount);
        summary.put("biometricVerified", verifiedCount);
        summary.put("incidents", incidentsCount);
        summary.put("disconnected", disconnectedCount);
        summary.put("examStatus", (exam != null) ? exam.getStatus() : "UNKNOWN");

        return org.springframework.http.ResponseEntity.ok(summary);
    }

    @PostMapping("/update-profile")
    public Map<String, Object> updateProfile(
            Authentication authentication,
            @RequestParam(value = "name", defaultValue = "") String name,
            @RequestParam(value = "email", required = false) String profileEmail,
            @RequestParam(value = "phone", defaultValue = "") String phone,
            @RequestParam(value = "university", defaultValue = "") String university,
            @RequestParam(value = "college", defaultValue = "") String college,
            @RequestParam(value = "employeeId", defaultValue = "") String employeeId,
            @RequestParam(value = "isBiometricEnrolled", required = false) Boolean isBiometricEnrolled, // New param
            @RequestParam(value = "photo", required = false) org.springframework.web.multipart.MultipartFile photo,
            @RequestParam(value = "appointmentLetter", required = false) org.springframework.web.multipart.MultipartFile appointmentLetter,
            @RequestParam(value = "idProof", required = false) org.springframework.web.multipart.MultipartFile idProof) {
        System.out.println("DEBUG: updateProfile called");
        System.out.println("DEBUG: isBiometricEnrolled=" + isBiometricEnrolled);
        Map<String, Object> response = new HashMap<>();
        try {
            if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return response;
            }
            String loggedInEmail = authentication.getName();
            User user = userRepository.findFirstByEmail(loggedInEmail).orElse(null);
            if (user == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            user.setName(name);
            if (profileEmail != null && !profileEmail.trim().isEmpty()) {
                user.setEmail(profileEmail.trim());
            }
            user.setPhoneNumber(phone);
            // university is intentionally NOT updated here — it is set by admin and read-only for supervisors
            user.setCollegeName(college);
            user.setEmployeeId(employeeId);
            user.setProfileCompleted(true);

            // Save biometric status ONLY if explicitly passed as true from frontend "Save"
            // action
            if (Boolean.TRUE.equals(isBiometricEnrolled)) {
                System.out.println("DEBUG: Saving biometric status");
                if (user.getBiometricHash() == null || user.getBiometricHash().isEmpty()) {
                    user.setBiometricHash(UUID.randomUUID().toString()); // Generate hash on save
                    user.setBiometricVerified(true);
                }
            }

            // File saving logic - save bytes to disk
            if (photo != null && !photo.isEmpty()) {
                String fileName = java.util.UUID.randomUUID() + "_" + photo.getOriginalFilename();
                saveFile(photo, fileName);
                user.setPhotoPath("uploads/" + fileName);
            }
            if (appointmentLetter != null && !appointmentLetter.isEmpty()) {
                String fileName = java.util.UUID.randomUUID() + "_" + appointmentLetter.getOriginalFilename();
                saveFile(appointmentLetter, fileName);
                user.setAppointmentLetterPath("uploads/" + fileName);
            }
            if (idProof != null && !idProof.isEmpty()) {
                String fileName = java.util.UUID.randomUUID() + "_" + idProof.getOriginalFilename();
                saveFile(idProof, fileName);
                user.setIdProofPath("uploads/" + fileName);
            }

            System.out.println("DEBUG: Saving user...");
            userRepository.save(user); // Persist all changes
            System.out.println("DEBUG: User saved successfully");

            response.put("success", true);
            response.put("message", "Profile updated successfully");
            return response;
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in updateProfile: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message",
                    "Error updating profile: " + (e.getMessage() != null ? e.getMessage() : "Unknown Error"));
            return response;
        }
    }

    // Helper method to save file locally
    private void saveFile(org.springframework.web.multipart.MultipartFile file, String fileName)
            throws java.io.IOException {
        java.io.File dir = new java.io.File(uploadDir).getAbsoluteFile();
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created && !dir.exists()) {
                System.err.println("Failed to create upload directory: " + dir.getAbsolutePath());
                throw new java.io.IOException("Failed to create upload directory: " + dir.getAbsolutePath());
            }
        }

        try (java.io.InputStream inputStream = file.getInputStream()) {
            java.io.File filePath = new java.io.File(dir, fileName);
            java.nio.file.Files.copy(inputStream, filePath.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException ioe) {
            throw new java.io.IOException("Could not save image file: " + fileName, ioe);
        }
    }

    @PostMapping("/update-password")
    public Map<String, Object> updatePassword(
            Authentication authentication,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword) {

        Map<String, Object> response = new HashMap<>();
        try {
            if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return response;
            }
            String email = authentication.getName();
            User user = userRepository.findFirstByEmail(email).orElse(null);

            if (user == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            // 1. Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                response.put("success", false);
                response.put("message", "Current password incorrect");
                return response;
            }

            // 2. Encode new password
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setFirstLogin(false);

            // 3. Save
            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Password updated successfully");
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error updating password: " + e.getMessage());
            return response;
        }
    }

    @PostMapping("/enroll-biometric")
    public Map<String, Object> enrollBiometric() {
        // PURE SIMULATION - Does not save to DB anymore
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Biometric scan simulation successful. Please save profile to commit.");
        return response;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.examauth.student_exam.repo.ExamRegistrationRepository examRegistrationRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.examauth.student_exam.service.NotificationService notificationService;

    @GetMapping("/exam-forms")
    public List<Map<String, Object>> getPendingExamForms(Authentication authentication) {

        // Resolve the logged-in supervisor's college AND type
        Long supervisorCollegeId = null;
        String supervisorTypeForForms = "EXAM"; // default — no college restriction
        if (authentication != null && authentication.getName() != null) {
            User supervisor = userRepository.findFirstByEmail(authentication.getName()).orElse(null);
            if (supervisor != null) {
                supervisorTypeForForms = supervisor.getSupervisorType() != null
                        ? supervisor.getSupervisorType() : "EXAM";
                if (supervisor.getCollege() != null) {
                    supervisorCollegeId = supervisor.getCollege().getId();
                }
            }
        }
        final Long collegeId = supervisorCollegeId;
        final String supType = supervisorTypeForForms;

        List<com.example.examauth.student_exam.model.ExamRegistration> applied = examRegistrationRepository
                .findByRegistrationStatus(
                        com.example.examauth.student_exam.model.ExamRegistration.RegistrationStatus.APPLIED);
        List<com.example.examauth.student_exam.model.ExamRegistration> pending = examRegistrationRepository
                .findByRegistrationStatus(
                        com.example.examauth.student_exam.model.ExamRegistration.RegistrationStatus.PENDING);
        List<com.example.examauth.student_exam.model.ExamRegistration> approved = examRegistrationRepository
                .findByRegistrationStatus(
                        com.example.examauth.student_exam.model.ExamRegistration.RegistrationStatus.APPROVED);

        List<com.example.examauth.student_exam.model.ExamRegistration> allPending = new ArrayList<>();
        allPending.addAll(applied);
        allPending.addAll(pending);
        allPending.addAll(approved);

        return allPending.stream()
                // ── College restriction (HEAD supervisors only) ─────────────────────
                .filter(reg -> {
                    // EXAM supervisors have no college restriction — they handle any college's exams
                    if ("EXAM".equalsIgnoreCase(supType))
                        return true;
                    // HEAD supervisor with no college assigned — show all (backward compat)
                    if (collegeId == null)
                        return true;
                    User student = userRepository.findById(reg.getStudentId()).orElse(null);
                    if (student == null)
                        return false;

                    // Primary: check student's own college_id
                    if (student.getCollege() != null) {
                        return collegeId.equals(student.getCollege().getId());
                    }

                    // Fallback: check the exam's college_id (student college_id may be NULL in DB
                    // even though the student belongs to this college via their exam registration)
                    com.example.examauth.model.Exam exam = examRepository.findById(reg.getExamId()).orElse(null);
                    if (exam != null && exam.getCollegeId() != null) {
                        return collegeId.equals(exam.getCollegeId());
                    }
                    com.example.examauth.student_exam.university.model.UniversityExam uExam =
                            universityExamRepository.findById(reg.getExamId()).orElse(null);
                    if (uExam != null && uExam.getCollegeId() != null) {
                        return collegeId.equals(uExam.getCollegeId());
                    }

                    // Cannot determine college — include rather than silently drop
                    return true;
                })
                // ────────────────────────────────────────────────────────────────────
                .map(reg -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", reg.getId());
                    map.put("studentId", reg.getStudentId());
                    map.put("examId", reg.getExamId());
                    map.put("prn", reg.getPrn());
                    map.put("fullName", reg.getFullName());
                    map.put("course", reg.getCourse());
                    map.put("examSession", reg.getExamSession());
                    map.put("selectedSubjects", reg.getSelectedSubjects());
                    map.put("status", reg.getRegistrationStatus().name());
                    // Extra fields for filtering
                    map.put("department", reg.getCourse());       // course = department (e.g. B.Tech Computer Science)
                    map.put("semester", reg.getExamSession());    // examSession = semester (e.g. Semester 8)

                    User studentUser = userRepository.findById(reg.getStudentId()).orElse(null);
                    if (studentUser != null) {
                        map.put("biometricVerified", studentUser.getBiometricVerified());
                        map.put("qrVerified", studentUser.getQrVerified());
                    } else {
                        map.put("biometricVerified", false);
                        map.put("qrVerified", false);
                    }

                    com.example.examauth.model.Exam exam = examRepository.findById(reg.getExamId()).orElse(null);
                    if (exam != null) {
                        map.put("examName", exam.getExamName());
                        map.put("examStatus", exam.getStatus());
                    } else {
                        com.example.examauth.student_exam.university.model.UniversityExam uExam = universityExamRepository
                                .findById(reg.getExamId()).orElse(null);
                        if (uExam != null) {
                            map.put("examName", uExam.getExamName());
                            map.put("examStatus", uExam.getStatus());
                        } else {
                            map.put("examName", "Unknown Exam");
                            map.put("examStatus", "UNKNOWN");
                        }
                    }
                    return map;
                }).collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/exam-forms/{id}/accept")
    public Map<String, Object> acceptExamForm(Authentication authentication, @PathVariable Long id) {
        try {
            com.example.examauth.student_exam.model.ExamRegistration reg = examRegistrationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Registration not found"));
            reg.setRegistrationStatus(com.example.examauth.student_exam.model.ExamRegistration.RegistrationStatus.APPROVED);
            reg.setPaymentStatus(com.example.examauth.student_exam.model.ExamRegistration.PaymentStatus.PAID);
            examRegistrationRepository.save(reg);

            String examName = "Exam ID " + reg.getExamId();

            Long currentUserId = null;
            String currentUserName = null;
            if (authentication != null && authentication.getName() != null) {
                User user = userRepository.findFirstByEmail(authentication.getName()).orElse(null);
                if (user != null) {
                    currentUserId = user.getUserId();
                    currentUserName = user.getName() != null ? user.getName() : "Supervisor";
                }
            }

            com.example.examauth.model.Exam exam = examRepository.findById(reg.getExamId()).orElse(null);
            if (exam != null) {
                examName = exam.getExamName();
                if (currentUserId != null && (exam.getSupervisorId() == null || exam.getSupervisorId() == 0L)) {
                    exam.setSupervisorId(currentUserId);
                    examRepository.save(exam);
                }
            } else {
                com.example.examauth.student_exam.university.model.UniversityExam uExam = universityExamRepository
                        .findById(reg.getExamId()).orElse(null);
                if (uExam != null) {
                    examName = uExam.getExamName();
                    if (currentUserId != null && (uExam.getSupervisorId() == null || uExam.getSupervisorId() == 0L)) {
                        uExam.setSupervisorId(currentUserId);
                        uExam.setSupervisorName(currentUserName);
                        universityExamRepository.save(uExam);
                    }
                }
            }

            notificationService.createNotification(reg.getStudentId(), "Exam Registration Verified",
                    "Your exam form for " + examName + " has been verified and accepted by the supervisor.");

            return Map.of("success", true, "message", "Form accepted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            return Map.of("success", false, "message", "Error: " + root.getMessage());
        }
    }

    @DeleteMapping("/exam-forms/{id}")
    public Map<String, Object> deleteExamForm(Authentication authentication, @PathVariable Long id) {
        try {
            com.example.examauth.student_exam.model.ExamRegistration reg = examRegistrationRepository.findById(id)
                    .orElse(null);
            if (reg == null) {
                return Map.of("success", false, "message", "Exam form not found");
            }
            Long studentId = reg.getStudentId();
            examRegistrationRepository.deleteById(id);
            notificationService.createNotification(studentId, "Exam Form Deleted",
                    "Your exam registration form has been removed by the supervisor.");
            return Map.of("success", true, "message", "Exam form deleted successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error deleting form: " + e.getMessage());
        }
    }

    @PostMapping("/verify-student-biometric")
    public ResponseEntity<Map<String, Object>> verifyStudentBiometric(
            Authentication authentication,
            @RequestBody Map<String, String> payload) {

        // 1. Role Check
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR"))) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Access denied"));
        }

        String studentIdStr = payload.get("studentId");
        String frontendFingerprint = payload.get("fingerprint");

        if (studentIdStr == null || frontendFingerprint == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing studentId or fingerprint"));
        }

        Long studentId;
        try {
            studentId = Long.parseLong(studentIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid student ID format"));
        }

        User student = userRepository.findById(studentId).orElse(null);

        if (student == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Student not found"));
        }

        // 2. Check Enrollment before verify
        if (!student.isBiometricEnrolled()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Student not enrolled for biometric"));
        }

        // 3. Prevent Fake Match (Add Salt & Hash)
        String localHash = com.example.examauth.util.HashUtil.sha256(frontendFingerprint + "_SECURE_SALT");

        // Use same timestamp generation to ensure format consistency
        String timestamp = java.time.LocalDateTime.now().toString();

        if (localHash.equals(student.getBiometricTemplateHash())) {
            // Optional: You could update student's last verified time here
            // student.setBiometricLastVerified(java.time.LocalDateTime.now());
            // userRepository.save(student);

            // 4. Standard Response Format (Verified)
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Biometric verified",
                    "attendanceStatus", "PRESENT",
                    "timestamp", timestamp));
        } else {
            // 4. Standard Response Format (Mismatch)
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Biometric mismatch. Try again.",
                    "attendanceStatus", "ABSENT",
                    "timestamp", timestamp));
        }
    }

    /**
     * Exam Report: Returns exam details + all seat allocations for a completed exam.
     * Used by the supervisor's "Download Report" feature.
     */
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.examauth.student_exam.repo.ExamSeatAllocationRepository examSeatAllocationRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.examauth.student_exam.repo.ExamHallRepository examHallRepository;

    @GetMapping("/exam/{examId}/report")
    public ResponseEntity<?> getExamReport(@PathVariable Long examId) {
        try {
            // Fetch exam details from UniversityExam table
            com.example.examauth.student_exam.university.model.UniversityExam uExam =
                    universityExamRepository.findById(examId).orElse(null);

            Map<String, Object> report = new HashMap<>();

            if (uExam != null) {
                report.put("examName", uExam.getSessionName());
                report.put("examDate", uExam.getExamDate() != null ? uExam.getExamDate().toString() : "N/A");
                report.put("startTime", uExam.getSchedule() != null && uExam.getSchedule().getStartTime() != null
                        ? uExam.getSchedule().getStartTime().toString() : "N/A");
                report.put("endTime", uExam.getSchedule() != null && uExam.getSchedule().getEndTime() != null
                        ? uExam.getSchedule().getEndTime().toString() : "N/A");
                report.put("mode", uExam.getMode() != null ? uExam.getMode() : "OFFLINE");
                report.put("course", uExam.getCourse() != null ? uExam.getCourse() : "");
                report.put("semester", uExam.getSemester() != null ? uExam.getSemester() : "");
                report.put("department", uExam.getDepartment() != null ? uExam.getDepartment() : "");
                report.put("centerName", uExam.getCenterName() != null ? uExam.getCenterName() : "");
                report.put("status", uExam.getStatus());
                report.put("examId", examId);
            } else {
                // Fallback to old exam table
                com.example.examauth.model.Exam exam = examRepository.findById(examId).orElse(null);
                if (exam != null) {
                    report.put("examName", exam.getExamName());
                    report.put("examDate", exam.getDate() != null ? exam.getDate().toString() : "N/A");
                    report.put("startTime", exam.getStartTime() != null ? exam.getStartTime().toString() : "N/A");
                    report.put("endTime", "N/A");
                    report.put("mode", exam.getMode() != null ? exam.getMode() : "OFFLINE");
                    report.put("status", exam.getStatus());
                    report.put("examId", examId);
                }
            }

            // Fetch all seat allocations for this exam
            List<com.example.examauth.student_exam.model.ExamSeatAllocation> seats =
                    examSeatAllocationRepository.findAllByExamId(examId);

            List<Map<String, Object>> studentList = seats.stream().map(s -> {
                Map<String, Object> row = new HashMap<>();
                row.put("rollNumber", s.getRollNumber() != null ? s.getRollNumber() : "N/A");
                row.put("seatNumber", s.getSeatNumber() != null ? s.getSeatNumber() : "N/A");
                row.put("prn", s.getPrn() != null ? s.getPrn() : "N/A");
                row.put("studentName", s.getStudentName() != null ? s.getStudentName() : "Student");
                row.put("hallName", s.getHallName() != null ? s.getHallName() : "N/A");
                row.put("collegeName", s.getCollegeName() != null ? s.getCollegeName() : "N/A");
                return row;
            }).toList();

            report.put("students", studentList);
            report.put("totalStudents", studentList.size());

            // Fetch hall names for this exam
            List<String> hallNames = seats.stream()
                    .map(com.example.examauth.student_exam.model.ExamSeatAllocation::getHallName)
                    .filter(h -> h != null)
                    .distinct().toList();
            report.put("halls", hallNames);
            report.put("totalHalls", hallNames.size());

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Supervisor Notifications ─────────────────────────────────────────────

    @GetMapping("/notifications")
    public ResponseEntity<?> getSupervisorNotifications(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User user = userRepository.findFirstByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        return ResponseEntity.ok(notificationService.getNotificationsForStudent(user.getUserId()));
    }

    @PostMapping("/notifications/mark-all-read")
    public ResponseEntity<?> markAllSupervisorNotificationsRead(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User user = userRepository.findFirstByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        notificationService.markAllAsRead(user.getUserId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}

