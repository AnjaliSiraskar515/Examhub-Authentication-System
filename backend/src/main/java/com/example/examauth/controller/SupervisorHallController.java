package com.example.examauth.controller;

import com.example.examauth.model.User;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.student_exam.model.ExamHall;
import com.example.examauth.student_exam.service.ExamHallService;
import com.example.examauth.student_exam.university.model.ExamCollegeMapping;
import com.example.examauth.student_exam.university.service.ExamCollegeMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supervisor/halls")
@RequiredArgsConstructor
@CrossOrigin(allowedHeaders = "*", originPatterns = "*", allowCredentials = "true")
@Slf4j
public class SupervisorHallController {

    private final ExamHallService examHallService;
    private final ExamCollegeMappingService mappingService;
    private final UserRepository userRepository;
    private final com.example.examauth.student_exam.university.repo.UniversityExamRepository universityExamRepository;

    private User getCurrentSupervisor() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) return null;
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User supervisor = userRepository.findFirstByEmailAndRole(email, "SUPERVISOR").orElse(null);
        if (supervisor != null && !"HEAD".equalsIgnoreCase(supervisor.getSupervisorType())) {
            return null; // Signals unauthorized inside the endpoints
        }
        return supervisor;
    }

    /**
     * Get all exam mappings assigned to this Head Supervisor.
     */
    @GetMapping("/mappings")
    public ResponseEntity<?> getAssignedMappings() {
        try {
            User supervisor = getCurrentSupervisor();
            if (supervisor == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            
            Long collegeId = supervisor.getCollege() != null ? supervisor.getCollege().getId() : null;
            String uniName = supervisor.getCollege() != null ? supervisor.getCollege().getUniversityName() : null;
            Long sId = supervisor.getUserId();
            
            String instCode = null;
            if (uniName != null) {
                instCode = userRepository.findByRole("UNIVERSITY_ADMIN").stream()
                    .filter(u -> uniName.equalsIgnoreCase(u.getUniversityName()))
                    .map(User::getInstitutionCode)
                    .findFirst().orElse(null);
            }
            final String finalInstCode = instCode;
            
            log.info("Supervisor {} belongs to College {} ({}), UniversityName: {}, InstCode: {}", supervisor.getName(), collegeId, supervisor.getCollege() != null ? supervisor.getCollege().getName() : "None", uniName, finalInstCode);
            
            List<Map<String, Object>> examList = universityExamRepository.findAll().stream()
                .filter(uExam -> {
                    if ("COMPLETED".equalsIgnoreCase(uExam.getStatus())) {
                        return false;
                    }
                    // Skip exams if the supervisor has no college — cannot configure halls without it
                    if (collegeId == null) return false;

                    boolean isCollegeExam = collegeId.equals(uExam.getCollegeId());
                    boolean isUniversityExam = finalInstCode != null && finalInstCode.equalsIgnoreCase(uExam.getInstitutionCode());
                    
                    log.info("Checking Exam {}: CollegeId={}, InstCode={}, isCollegeExam={}, isUniversityExam={}", 
                        uExam.getSessionName(), uExam.getCollegeId(), uExam.getInstitutionCode(), isCollegeExam, isUniversityExam);

                    if (isCollegeExam || isUniversityExam) {
                        return true;
                    }
                    return (uExam.getSupervisorId() != null && uExam.getSupervisorId().equals(sId)) ||
                           (uExam.getSupervisorIds() != null && uExam.getSupervisorIds().contains(sId));
                })
                .map(exam -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("examId", exam.getId());
                    map.put("examName", exam.getExamName());
                    // ALWAYS use the supervisor's own collegeId — never null — so
                    // hall creation always sends a valid collegeId to the backend.
                    map.put("collegeId", collegeId);
                    return map;
                })
                .toList();
                
            return ResponseEntity.ok(examList);
        } catch (Exception e) {
            log.error("Error fetching supervisor mappings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a new ExamHall physical configuration for an exam.
     */
    @PostMapping("/")
    public ResponseEntity<?> createExamHall(@RequestBody ExamHall hall) {
        try {
            User supervisor = getCurrentSupervisor();
            if (supervisor == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            
            // Enforce security: Supervisor can only create halls for their college
            if (supervisor.getCollege() == null || !supervisor.getCollege().getId().equals(hall.getCollegeId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Cannot create halls for another college"));
            }

            hall.setCreatedBySupervisorId(supervisor.getUserId());

            // Validate and populate exam supervisor (Option C: university-scoped flexibility)
            if (hall.getExamSupervisorId() != null) {
                User examSupervisor = userRepository.findById(hall.getExamSupervisorId()).orElse(null);
                if (examSupervisor == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Exam Supervisor not found"));
                }
                if (!"EXAM".equalsIgnoreCase(examSupervisor.getSupervisorType())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Assigned user must be an EXAM supervisor"));
                }
                // University-scoped: exam supervisor must belong to a college under the same university
                String headUniName = supervisor.getCollege() != null ? supervisor.getCollege().getUniversityName() : null;
                String supUniName = examSupervisor.getCollege() != null ? examSupervisor.getCollege().getUniversityName() : null;
                if (headUniName != null && !headUniName.equalsIgnoreCase(supUniName)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Exam Supervisor must belong to the same university"));
                }
                if (hall.getExamSupervisorName() == null) {
                    hall.setExamSupervisorName(examSupervisor.getName());
                }

                // Prevent assigning the same supervisor to multiple halls for the SAME exam
                List<ExamHall> existingHalls = examHallService.getHallsByExamAndCollege(hall.getExamId(), hall.getCollegeId());
                boolean alreadyAssigned = existingHalls.stream()
                        .anyMatch(h -> hall.getExamSupervisorId().equals(h.getExamSupervisorId()));
                if (alreadyAssigned) {
                    return ResponseEntity.badRequest().body(Map.of("error", "This supervisor is already assigned to another hall for this exam."));
                }
            }

            ExamHall created = examHallService.createHall(hall);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Failed to create exam hall: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all ExamHalls configured for an exam + college combination.
     */
    @GetMapping("/")
    public ResponseEntity<?> getExamHalls(@RequestParam Long examId, @RequestParam Long collegeId) {
        try {
            User supervisor = getCurrentSupervisor();
            if (supervisor == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            
            // Enforce security
            if (supervisor.getCollege() == null || !supervisor.getCollege().getId().equals(collegeId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Cannot view halls for another college"));
            }

            List<ExamHall> halls = examHallService.getHallsByExamAndCollege(examId, collegeId);
            return ResponseEntity.ok(halls);
        } catch (Exception e) {
            log.error("Error fetching exam halls: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete an existing ExamHall.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExamHall(@PathVariable Long id) {
        try {
            User supervisor = getCurrentSupervisor();
            if (supervisor == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            
            ExamHall hall = examHallService.getHallById(id).orElse(null);
            if (hall == null) return ResponseEntity.notFound().build();
            
            // Enforce security
            if (supervisor.getCollege() == null || !supervisor.getCollege().getId().equals(hall.getCollegeId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Cannot delete halls for another college"));
            }
            
            examHallService.deleteHall(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Hall deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting exam hall: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all EXAM-type supervisors from the same university as this Head Supervisor,
     * EXCLUDING supervisors from the same department as the exam being configured.
     * (Cross-departmental invigilation rule — Option C + anti-bias filter)
     */
    @GetMapping("/exam-supervisors")
    public ResponseEntity<?> getExamSupervisors(@RequestParam(required = false) Long examId) {
        try {
            User supervisor = getCurrentSupervisor();
            if (supervisor == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            // Get the university name of the head supervisor's college
            String universityName = supervisor.getCollege() != null
                    ? supervisor.getCollege().getUniversityName()
                    : null;

            if (universityName == null) {
                return ResponseEntity.ok(List.of());
            }

            // Resolve exam department to exclude (cross-departmental invigilation)
            String examDepartment = null;
            if (examId != null) {
                examDepartment = universityExamRepository.findById(examId)
                        .map(e -> e.getDepartment())
                        .orElse(null);
                log.info("Cross-dept filter: exam {} has department '{}'", examId, examDepartment);
            }

            final String uniName = universityName;
            final String excludedDept = examDepartment;

            // Return all active EXAM supervisors from same university,
            // excluding those from the same department as the exam
            List<Map<String, Object>> result = userRepository.findAll().stream()
                .filter(u -> "SUPERVISOR".equalsIgnoreCase(u.getRole())
                        && "EXAM".equalsIgnoreCase(u.getSupervisorType())
                        && (u.getStatus() == null || "active".equalsIgnoreCase(u.getStatus()))
                        && u.getCollege() != null
                        && uniName.equalsIgnoreCase(u.getCollege().getUniversityName())
                        // Exclude supervisors whose department matches the exam department
                        && (excludedDept == null
                            || u.getDepartment() == null
                            || !excludedDept.equalsIgnoreCase(u.getDepartment())))
                .map(u -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("userId", u.getUserId());
                    m.put("name", u.getName());
                    m.put("email", u.getEmail());
                    m.put("college", u.getCollege().getName());
                    m.put("department", u.getDepartment() != null ? u.getDepartment() : "");
                    m.put("supervisorType", u.getSupervisorType());
                    m.put("status", u.getStatus());
                    return m;
                })
                .toList();

            log.info("Returning {} eligible exam supervisors (exam dept '{}' excluded)", result.size(), excludedDept);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching exam supervisors: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

