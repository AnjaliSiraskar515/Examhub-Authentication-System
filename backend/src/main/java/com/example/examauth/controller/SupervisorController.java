package com.example.examauth.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.example.examauth.model.User;

@RestController
@RequestMapping("/api/supervisor")
@CrossOrigin // Allow frontend access
public class SupervisorController {

    private final com.example.examauth.repo.UserRepository userRepository;
    private final com.example.examauth.repo.QrRepository qrRepository;
    private final com.example.examauth.repo.ExamRepository examRepository;

    public SupervisorController(com.example.examauth.repo.UserRepository userRepository,
            com.example.examauth.repo.QrRepository qrRepository,
            com.example.examauth.repo.ExamRepository examRepository) {
        this.userRepository = userRepository;
        this.qrRepository = qrRepository;
        this.examRepository = examRepository;
    }

    @org.springframework.beans.factory.annotation.Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping("/profile")
    public Map<String, Object> getProfile() {
        // Fetch real counts
        // Fetch real counts
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        long totalScans = qrRepository.countByUsedAndIssuedAtAfter(true, startOfDay);
        long fullyVerifiedCount = userRepository.countByRoleAndBiometricVerified("STUDENT", true); // Fix: Only count
                                                                                                   // students
        long totalStudents = userRepository.countByRole("STUDENT");

        System.out.println("DEBUG: Profile Stats Calculation");
        System.out.println("DEBUG: totalScans=" + totalScans);
        System.out.println("DEBUG: fullyVerifiedCount=" + fullyVerifiedCount);
        System.out.println("DEBUG: totalStudents=" + totalStudents);

        long pendingCount = Math.max(0, totalStudents - fullyVerifiedCount);

        Map<String, Object> profile = new HashMap<>();
        profile.put("scansToday", totalScans); // Purple card: Total Scans
        profile.put("pendingVerifications", pendingCount); // Red card: Pending
        profile.put("studentsCount", totalStudents); // Profile Stats: Total Assigned
        profile.put("verifiedStudentsCount", fullyVerifiedCount); // Green card: Real Verified Count
        profile.put("assignedExamsCount", examRepository.count()); // Blue card: Exams

        // Supervisor Identity & Affiliation
        User user = userRepository.findByEmail("supervisor@examhub.edu").orElse(new User());
        profile.put("name", user.getName() != null ? user.getName() : "Dr. Supervisor");
        profile.put("email", user.getEmail() != null ? user.getEmail() : "supervisor@examhub.edu");
        profile.put("phone", user.getPhoneNumber());
        profile.put("university", user.getUniversityName());
        profile.put("college", user.getCollegeName());
        profile.put("designation", user.getDesignation());
        profile.put("employeeId", user.getEmployeeId());
        profile.put("avatar", user.getPhotoPath()); // Add avatar path
        profile.put("biometricEnrolled", user.getBiometricHash() != null && !user.getBiometricHash().isEmpty());

        List<Map<String, String>> docs = new ArrayList<>();
        if (user.getAppointmentLetterPath() != null)
            docs.add(Map.of("filename", "Appointment Letter", "status", "Uploaded"));
        if (user.getIdProofPath() != null)
            docs.add(Map.of("filename", "ID Proof", "status", "Uploaded"));
        if (docs.isEmpty())
            docs.add(Map.of("filename", "Supervisor_Guidelines.pdf", "status", "Pending"));

        profile.put("documents", docs);
        return profile;
    }

    @GetMapping("/exams")
    public List<Map<String, Object>> getExams() {
        return examRepository.findAll().stream().map(exam -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", exam.getExamId());
            map.put("title", exam.getExamName()); // Corrected field name
            map.put("date", exam.getDate().toString()); // Corrected field name
            map.put("duration", exam.getDurationMinutes());
            map.put("startTime", exam.getStartTime() != null ? exam.getStartTime().toString() : "TBD");
            map.put("mode", exam.getMode());
            map.put("status", exam.getStatus());
            map.put("location", exam.getLocation());
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/students")
    public List<Map<String, Object>> getStudents(@RequestParam(value = "examId", required = false) Long examId) {
        // Determine target exam name for filtering
        String targetExamName = null;
        String examStatus = "UPCOMING";
        if (examId != null) {
            com.example.examauth.model.Exam exam = examRepository.findById(examId).orElse(null);
            if (exam != null) {
                targetExamName = exam.getExamName();
                examStatus = exam.getStatus();
            }
        }
        final String filterExamName = targetExamName;
        final boolean isExamEnded = "ENDED".equals(examStatus);

        return userRepository.findByRole("STUDENT").stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getUserId());
            map.put("name", user.getName());
            map.put("email", user.getEmail());
            map.put("regno", user.getUsername());

            // Mock Exam Assignment for Demo
            String assignedExam = "Advanced Java Programming";
            if (user.getUsername() != null) {
                String lowerUser = user.getUsername().trim().toLowerCase();
                if (lowerUser.startsWith("student")) {
                    assignedExam = "Theory of Computation";
                } else if (lowerUser.startsWith("os_student") || lowerUser.startsWith("os")) {
                    assignedExam = "Operating System";
                }
            }
            map.put("examName", assignedExam);

            // Add verification status
            map.put("qrVerified", user.getQrVerified());
            map.put("biometricVerified", user.getBiometricVerified());

            if (isExamEnded) {
                // STABLE STATUS FOR ENDED EXAMS
                map.put("warningCount", 0);
                map.put("lastActivityTime", "Finalized");

                // Logic to match exam summary
                if (Boolean.TRUE.equals(user.getBiometricVerified())) {
                    map.put("status", "SUBMITTED");
                } else {
                    // Deterministic fallback for unverified but submitted
                    int hash = (user.getUsername() != null) ? user.getUsername().hashCode() : 0;
                    if (Math.abs(hash) % 10 < 8) {
                        map.put("status", "SUBMITTED");
                    } else {
                        map.put("status", "ABSENT");
                    }
                }
            } else {
                // LIVE RANDOM SIMULATION
                map.put("warningCount", (int) (Math.random() * 3)); // Random 0-2 warnings
                map.put("lastActivityTime",
                        java.time.LocalTime.now().minusSeconds((long) (Math.random() * 300)).toString());

                // Simulate Status
                String[] statuses = { "ACTIVE", "IDLE", "DISCONNECTED", "SUSPICIOUS" };
                String status = statuses[(int) (Math.random() * statuses.length)];
                // Override if verified
                if (Boolean.TRUE.equals(user.getBiometricVerified())) {
                    status = "ACTIVE";
                }
                map.put("status", status);
            }

            return map;
        })
                .filter(map -> {
                    if (filterExamName == null)
                        return true; // No filter applied
                    return filterExamName.equals(map.get("examName"));
                })
                .collect(java.util.stream.Collectors.toList());
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
        com.example.examauth.model.Exam exam = examRepository.findById(id).orElseThrow();
        exam.setStatus(status);
        examRepository.save(exam);

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
            @RequestParam(value = "name", defaultValue = "") String name,
            @RequestParam(value = "phone", defaultValue = "") String phone,
            @RequestParam(value = "university", defaultValue = "") String university,
            @RequestParam(value = "college", defaultValue = "") String college,
            @RequestParam(value = "designation", defaultValue = "") String designation,
            @RequestParam(value = "employeeId", defaultValue = "") String employeeId,
            @RequestParam(value = "isBiometricEnrolled", required = false) Boolean isBiometricEnrolled, // New param
            @RequestParam(value = "photo", required = false) org.springframework.web.multipart.MultipartFile photo,
            @RequestParam(value = "appointmentLetter", required = false) org.springframework.web.multipart.MultipartFile appointmentLetter,
            @RequestParam(value = "idProof", required = false) org.springframework.web.multipart.MultipartFile idProof) {
        System.out.println("DEBUG: updateProfile called");
        System.out.println("DEBUG: isBiometricEnrolled=" + isBiometricEnrolled);
        try {
            Optional<User> userOpt = userRepository.findByEmail("supervisor@examhub.edu");
            User user;

            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                // If not found by email, try by username to avoid unique constraint if exists
                Optional<User> userByUsername = userRepository.findByUsername("supervisor_admin");
                if (userByUsername.isPresent()) {
                    user = userByUsername.get();
                    // If found by username but email is different/missing, update it
                    if (!"supervisor@examhub.edu".equals(user.getEmail())) {
                        user.setEmail("supervisor@examhub.edu");
                    }
                } else {
                    System.out.println("DEBUG: Creating new user");
                    user = new User();
                    user.setEmail("supervisor@examhub.edu");
                    user.setRole("SUPERVISOR");
                    user.setPassword("supervisor123");
                    user.setStatus("ACTIVE");
                    user.setUsername("supervisor_admin");
                }
            }

            user.setName(name);
            user.setPhoneNumber(phone);
            user.setUniversityName(university);
            user.setCollegeName(college);
            user.setDesignation(designation);
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            return response;
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in updateProfile: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
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

    @PostMapping("/enroll-biometric")
    public Map<String, Object> enrollBiometric() {
        // PURE SIMULATION - Does not save to DB anymore
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Biometric scan simulation successful. Please save profile to commit.");
        return response;
    }
}
