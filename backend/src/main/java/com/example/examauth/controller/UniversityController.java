package com.example.examauth.controller;

import com.example.examauth.model.Exam;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.service.ExamService;
import com.example.examauth.dto.ExamResponseDTO;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import com.example.examauth.student_exam.university.model.UniversityExam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.example.examauth.student_exam.repo.ExamRegistrationRepository;
import com.example.examauth.student_exam.model.ExamRegistration;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/university")
public class UniversityController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamService examService;

    @Autowired
    private UniversityExamRepository universityExamRepository;

    @Autowired
    private ExamRegistrationRepository examRegistrationRepository;

    @Autowired
    private com.example.examauth.student_exam.university.repo.ExamCollegeMappingRepository examCollegeMappingRepository;

    @Autowired
    private com.example.examauth.student_exam.service.NotificationService notificationService;

    // Helper: get the university name of the currently authenticated UNIVERSITY_ADMIN
    private String getCurrentAdminUniversityName() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        String role = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        com.example.examauth.model.User admin = userRepository.findFirstByEmailAndRole(email, role).orElse(null);
        if (admin != null && "UNIVERSITY_ADMIN".equals(admin.getRole())) {
            String n = admin.getUniversityName(); return n != null ? n.trim() : null;
        }
        return null; // Not a UNIVERSITY_ADMIN — SUPER_ADMIN passes through
    }

    private String getCurrentAdminInstitutionCode() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        String role = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        com.example.examauth.model.User admin = userRepository.findFirstByEmailAndRole(email, role).orElse(null);
        if (admin != null && "UNIVERSITY_ADMIN".equals(admin.getRole())) {
            String c = admin.getInstitutionCode(); return c != null ? c.trim() : null;
        }
        return null;
    }

    // GET /api/university/{id}/students
    @GetMapping("/{id}/students")
    public ResponseEntity<?> getStudents(@PathVariable Long id) {
        String myUniv = getCurrentAdminUniversityName();
        boolean isUnivAdmin = myUniv != null; // null means SUPER_ADMIN

        List<Map<String, Object>> students = userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .filter(u -> {
                    if (!isUnivAdmin) return true; // SUPER_ADMIN sees all
                    // UNIVERSITY_ADMIN: if no universityName set in their profile, block everything
                    if (myUniv.isEmpty()) return false;
                    String userUniv = u.getUniversityName() != null ? u.getUniversityName().trim() : "";
                    String colUniv = (u.getCollege() != null && u.getCollege().getUniversityName() != null) ? u.getCollege().getUniversityName().trim() : "";
                    return myUniv.equalsIgnoreCase(userUniv) || myUniv.equalsIgnoreCase(colUniv);
                })
                .map(u -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("userId", u.getUserId());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("major", u.getMajor());
                    map.put("year", u.getYear());
                    map.put("department", u.getDepartment());
                    map.put("status", u.getStatus());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    // GET /api/university/{id}/staff
    @GetMapping("/{id}/staff")
    public ResponseEntity<?> getStaff(@PathVariable Long id) {
        String myUniv = getCurrentAdminUniversityName();
        boolean isUnivAdmin = myUniv != null;

        List<Map<String, Object>> staff = userRepository.findAll().stream()
                .filter(u -> "SUPERVISOR".equalsIgnoreCase(u.getRole()))
                .filter(u -> {
                    if (!isUnivAdmin) return true;
                    if (myUniv.isEmpty()) return false;
                    String userUniv = u.getUniversityName() != null ? u.getUniversityName().trim() : "";
                    String colUniv = (u.getCollege() != null && u.getCollege().getUniversityName() != null) ? u.getCollege().getUniversityName().trim() : "";
                    return myUniv.equalsIgnoreCase(userUniv) || myUniv.equalsIgnoreCase(colUniv);
                })
                .map(u -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("userId", u.getUserId());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("status", u.getStatus());
                    map.put("department", u.getDepartment());
                    map.put("role", u.getRole());
                    map.put("supervisorType", u.getSupervisorType());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(staff);
    }

    // POST /api/university/{id}/exam
    @PostMapping("/{id}/exam")
    public ResponseEntity<?> createExam(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Exam exam = new Exam();
            exam.setCollegeId(id);
            exam.setExamName((String) request.get("examName"));
            // Use provided institution or default
            exam.setInstitutionName((String) request.getOrDefault("institutionName", "University"));

            exam.setDate(java.time.LocalDate.parse((String) request.get("date")));
            exam.setStartTime(java.time.LocalTime.parse((String) request.get("startTime")));

            // Handle potentially different number types from JSON
            Object durationObj = request.get("durationMinutes");
            if (durationObj instanceof Number) {
                exam.setDurationMinutes(((Number) durationObj).intValue());
            } else {
                exam.setDurationMinutes(Integer.parseInt(String.valueOf(durationObj)));
            }

            exam.setMode((String) request.get("mode"));
            exam.setLocation((String) request.get("location"));
            exam.setStatus("upcoming");

            if (request.containsKey("supervisorId") && request.get("supervisorId") != null) {
                exam.setSupervisorId(Long.valueOf(request.get("supervisorId").toString()));
            }
            if (request.containsKey("supervisorName") && request.get("supervisorName") != null) {
                exam.setSupervisorName((String) request.get("supervisorName"));
            }

            Exam savedExam = examService.createExam(exam);
            return ResponseEntity.ok(Map.of("message", "Exam created successfully", "examId", savedExam.getExamId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create exam: " + e.getMessage()));
        }
    }

    // GET /api/university/{id}/exam
    @GetMapping("/{id}/exam")
    public ResponseEntity<?> getExams(@PathVariable Long id) {
        String myUniv = getCurrentAdminUniversityName();
        String myInstCode = getCurrentAdminInstitutionCode();
        boolean isUnivAdmin = (myUniv != null || myInstCode != null);

        List<ExamResponseDTO> unifiedExams = new ArrayList<>();

        // 1. Fetch Legacy Exams - filter by institutionName matching universityName
        List<Exam> legacyExams = examService.getAllExams();
        if (isUnivAdmin) {
            // Fail-closed: if universityName is blank/null, show nothing
            if (myUniv == null || myUniv.isEmpty()) {
                legacyExams = new ArrayList<>();
            } else {
                legacyExams = legacyExams.stream()
                    .filter(e -> e.getInstitutionName() != null && myUniv.equalsIgnoreCase(e.getInstitutionName().trim()))
                    .collect(java.util.stream.Collectors.toList());
            }
        }
        for (Exam e : legacyExams) {
            unifiedExams.add(ExamResponseDTO.builder()
                    .sourceId("LEGACY_" + e.getExamId())
                    .id(e.getExamId())
                    .source("LEGACY")
                    .examName(e.getExamName())
                    .date(e.getDate())
                    .startTime(e.getStartTime())
                    .durationMinutes(e.getDurationMinutes() != null ? e.getDurationMinutes() : 0)
                    .status(e.getStatus())
                    .mode(e.getMode())
                    .location(e.getLocation() != null ? e.getLocation() : "N/A")
                    .build());
        }

        // 2. Fetch University Exams (New) - filter by institutionCode
        List<UniversityExam> universityExams = universityExamRepository.findAll();
        if (isUnivAdmin) {
            if (myInstCode == null || myInstCode.isEmpty()) {
                universityExams = new ArrayList<>();
            } else {
                universityExams = universityExams.stream()
                    .filter(e -> e.getInstitutionCode() != null && myInstCode.equalsIgnoreCase(e.getInstitutionCode().trim()))
                    .collect(java.util.stream.Collectors.toList());
            }
        }
        for (UniversityExam ue : universityExams) {
            unifiedExams.add(ExamResponseDTO.builder()
                    .sourceId("UNIV_" + ue.getId())
                    .id(ue.getId())
                    .source("UNIVERSITY")
                    .examName(ue.getSessionName())
                    .date(ue.getSchedule() != null ? ue.getSchedule().getExamDate() : null)
                    .startTime(ue.getSchedule() != null ? ue.getSchedule().getStartTime() : null)
                    .durationMinutes(0)
                    .status(ue.getStatus())
                    .mode(ue.getMode())
                    .location(ue.getCenterName() != null ? ue.getCenterName() : "N/A")
                    .build());
        }

        // 3. Mark exams dynamically based on date and time constraints
        LocalDateTime now = LocalDateTime.now();
        for (ExamResponseDTO dto : unifiedExams) {
            if (dto.getDate() != null && !"DRAFT".equalsIgnoreCase(dto.getStatus())) {
                LocalDateTime examDateTime = LocalDateTime.of(dto.getDate(), dto.getStartTime() != null ? dto.getStartTime() : java.time.LocalTime.MIN);
                int duration = dto.getDurationMinutes() != null && dto.getDurationMinutes() > 0 ? dto.getDurationMinutes() : 180; // default 3 hours
                LocalDateTime examEndTime = examDateTime.plusMinutes(duration);
                
                if (now.isBefore(examDateTime)) {
                    dto.setStatus("UPCOMING");
                } else if (now.isAfter(examEndTime)) {
                    dto.setStatus("COMPLETED");
                } else {
                    dto.setStatus("LIVE");
                }
            }
        }

        // 4. Sort by Date + Time (Latest first)
        unifiedExams.sort(Comparator.comparing((ExamResponseDTO e) -> {
            if (e.getDate() == null)
                return LocalDateTime.MIN;
            return LocalDateTime.of(e.getDate(), e.getStartTime() != null ? e.getStartTime() : java.time.LocalTime.MIN);
        }).reversed());

        return ResponseEntity.ok(unifiedExams);
    }

    @DeleteMapping("/{univId}/exam/{id}")
    public ResponseEntity<?> deleteExam(@PathVariable Long univId, @PathVariable String id) {
        try {
            if (id.startsWith("LEGACY_")) {
                Long examId = Long.parseLong(id.substring(7));
                examService.deleteExam(examId);
                return ResponseEntity.ok(Map.of("message", "Legacy exam deleted"));
            } else if (id.startsWith("UNIV_")) {
                Long targetUnivId = Long.parseLong(id.substring(5));
                universityExamRepository.deleteById(targetUnivId);
                return ResponseEntity.ok(Map.of("message", "University exam deleted"));
            }
            return ResponseEntity.status(404).body(Map.of("error", "Exam not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete exam: " + e.getMessage()));
        }
    }

    // POST /api/university/release-hallticket/{examId}
    @PostMapping("/release-hallticket/{examId}")
    public ResponseEntity<?> releaseHallticket(@PathVariable Long examId) {
        try {
            // Validation: Ensure at least one college has generated seats
            List<com.example.examauth.student_exam.university.model.ExamCollegeMapping> mappings = examCollegeMappingRepository.findAllByExamId(examId);
            if (!mappings.isEmpty()) {
                boolean hasSeatsGenerated = mappings.stream()
                    .anyMatch(m -> m.getStatus() == com.example.examauth.student_exam.university.model.ExamCollegeMapping.MappingStatus.SEATS_GENERATED || 
                                   m.getStatus() == com.example.examauth.student_exam.university.model.ExamCollegeMapping.MappingStatus.COMPLETED);
                
                if (!hasSeatsGenerated) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Cannot release hall tickets. Hall and seat allocation must be completed for at least one college first."));
                }
            }

            List<ExamRegistration> registrations = examRegistrationRepository.findByExamIdAndRegistrationStatus(examId,
                    ExamRegistration.RegistrationStatus.APPROVED);

            // FALLBACK FOR MOCK DATA MISMATCH: if the university clicks release but the
            // mock student registered to a deleted/phantom exam ID.
            if (registrations.isEmpty()) {
                registrations = examRegistrationRepository
                        .findByRegistrationStatus(ExamRegistration.RegistrationStatus.APPROVED);
            }

            for (ExamRegistration reg : registrations) {
                reg.setHallTicketReleased(true);
                // Trigger notification for the student
                String examName = reg.getExamSession() != null ? reg.getExamSession() : "your exam";
                notificationService.createNotification(
                    reg.getStudentId(), 
                    "Hall Ticket Released", 
                    "Your hall ticket for " + examName + " has been released. Please download it from your dashboard."
                );
            }
            examRegistrationRepository.saveAll(registrations);
            return ResponseEntity.ok(Map.of("message", "Hall Ticket released successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to release hall ticket: " + e.getMessage()));
        }
    }
}
