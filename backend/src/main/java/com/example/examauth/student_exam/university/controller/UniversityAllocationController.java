package com.example.examauth.student_exam.university.controller;

import com.example.examauth.student_exam.service.SeatAllocationService;
import com.example.examauth.student_exam.university.model.ExamCollegeMapping;
import com.example.examauth.student_exam.university.service.ExamCollegeMappingService;
import com.example.examauth.student_exam.repo.ExamSeatAllocationRepository;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.examauth.student_exam.dto.SeatAllocationDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/university/allocation")
@RequiredArgsConstructor
@CrossOrigin(allowedHeaders = "*", originPatterns = "*", allowCredentials = "true")
@Slf4j
public class UniversityAllocationController {

    private final ExamCollegeMappingService mappingService;
    private final SeatAllocationService seatAllocationService;
    private final ExamSeatAllocationRepository seatAllocationRepository;
    private final UserRepository userRepository;

    private User getCurrentAdmin() {
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) return null;
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findFirstByEmailAndRole(email, "UNIVERSITY_ADMIN").orElse(null);
        return admin;
    }

    /**
     * Map a university exam to a college and assign a Head Supervisor.
     */
    @PostMapping("/mappings")
    public ResponseEntity<?> createMapping(@RequestBody ExamCollegeMapping mapping) {
        try {
            User admin = getCurrentAdmin(); // Enforce strictly University Admin
            if (admin == null) {
                return ResponseEntity.status(403).body(Map.of("error", "Strictly restricted to University Admin"));
            }

            // Fill denormalized names and enforce HEAD supervisor validation
            User headSupervisor = userRepository.findById(mapping.getHeadSupervisorId()).orElse(null);
            if (headSupervisor == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Head Supervisor not found"));
            }
            if (!"HEAD".equalsIgnoreCase(headSupervisor.getSupervisorType())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Assigned user must be a HEAD supervisor"));
            }
            if (headSupervisor.getCollege() == null || !headSupervisor.getCollege().getId().equals(mapping.getCollegeId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Head Supervisor must belong to the selected college"));
            }

            if (mapping.getHeadSupervisorName() == null) mapping.setHeadSupervisorName(headSupervisor.getName());
            if (mapping.getCollegeName() == null) mapping.setCollegeName(headSupervisor.getCollege().getName());
            
            ExamCollegeMapping created = mappingService.createMapping(mapping);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Failed to map exam to college: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * AUTO-MAP: Finds all HEAD supervisors under the admin's institution and
     * automatically creates exam-college mappings. Skips already-mapped colleges.
     */
    @PostMapping("/auto-map/{examId}")
    public ResponseEntity<?> autoMapAllColleges(@PathVariable Long examId) {
        try {
            User admin = getCurrentAdmin();
            if (admin == null) return ResponseEntity.status(403).body(Map.of("error", "Strictly restricted to University Admin"));

            String institutionCode = admin.getInstitutionCode();
            if (institutionCode == null || institutionCode.trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "University Admin has no institution code assigned"));

            String myUniv = admin.getUniversityName();
            List<User> headSupervisors = userRepository.findAll().stream()
                .filter(u -> {
                    String r = u.getRole() != null ? u.getRole().trim().toLowerCase() : "";
                    return r.contains("supervisor") || r.contains("staff") || r.contains("faculty") || u.getSupervisorType() != null;
                })
                .filter(u -> u.getSupervisorType() != null && "HEAD".equalsIgnoreCase(u.getSupervisorType().trim()))
                .filter(u -> u.getStatus() != null && "active".equalsIgnoreCase(u.getStatus().trim()))
                .filter(u -> myUniv != null && (
                        myUniv.equalsIgnoreCase(u.getUniversityName()) || 
                        (u.getCollege() != null && myUniv.equalsIgnoreCase(u.getCollege().getUniversityName())) ||
                        (u.getInstitutionCode() != null && u.getInstitutionCode().equals(institutionCode))
                ))
                .toList();

            if (headSupervisors.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "mapped", 0, "skipped", 0, "noCollege", 0,
                    "message", "No HEAD supervisors found. Please import staff via CSV first.",
                    "details", List.of()
                ));
            }

            int mapped = 0, skipped = 0, noCollege = 0;
            List<Map<String, Object>> details = new ArrayList<>();

            List<Long> alreadyMappedCollegeIds = mappingService.getMappingsByExam(examId)
                .stream().map(ExamCollegeMapping::getCollegeId).toList();

            for (User head : headSupervisors) {
                Map<String, Object> row = new HashMap<>();
                row.put("supervisor", head.getName());

                if (head.getCollege() == null) {
                    noCollege++;
                    row.put("college", "N/A");
                    row.put("status", "SKIPPED");
                    row.put("reason", "No college assigned to this supervisor");
                    details.add(row);
                    continue;
                }

                Long collegeId = head.getCollege().getId();
                String collegeName = head.getCollege().getName();
                row.put("college", collegeName);

                if (alreadyMappedCollegeIds.contains(collegeId)) {
                    skipped++;
                    row.put("status", "ALREADY_MAPPED");
                    row.put("reason", "College already mapped for this exam");
                    details.add(row);
                    continue;
                }

                ExamCollegeMapping newMapping = new ExamCollegeMapping();
                newMapping.setExamId(examId);
                newMapping.setCollegeId(collegeId);
                newMapping.setCollegeName(collegeName);
                newMapping.setHeadSupervisorId(head.getUserId());
                newMapping.setHeadSupervisorName(head.getName());
                newMapping.setStatus(ExamCollegeMapping.MappingStatus.PENDING);
                mappingService.createMapping(newMapping);
                mapped++;
                
                row.put("status", "MAPPED");
                row.put("reason", "Successfully auto-mapped");
                details.add(row);
            }

            return ResponseEntity.ok(Map.of(
                "mapped", mapped, "skipped", skipped, "noCollege", noCollege,
                "message", "Auto-map complete: " + mapped + " mapped, " + skipped + " already existed, " + noCollege + " missing college",
                "details", details
            ));

        } catch (Exception e) {
            log.error("Auto-map failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all college mappings for a specific exam.
     */
    @GetMapping("/mappings/exam/{examId}")
    public ResponseEntity<List<ExamCollegeMapping>> getMappingsByExam(@PathVariable Long examId) {
        return ResponseEntity.ok(mappingService.getMappingsByExam(examId));
    }

    /**
     * Generate seat allocations and roll numbers for a specific college and exam.
     * This is triggered by the University Admin after halls are configured.
     */
    @PostMapping("/generate-seats")
    public ResponseEntity<?> generateSeats(@RequestParam Long examId, @RequestParam Long collegeId) {
        try {
            User admin = getCurrentAdmin(); // Enforce strictly University Admin
            if (admin == null) {
                return ResponseEntity.status(403).body(Map.of("error", "Strictly restricted to University Admin"));
            }
            seatAllocationService.generateSeatAllocation(examId, collegeId);
            return ResponseEntity.ok(Map.of("message", "Seat allocation generated successfully."));
        } catch (Exception e) {
            log.error("Failed to generate seats: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all seat allocations for a given exam and college.
     */
    @GetMapping("/seats")
    public ResponseEntity<?> getSeats(
            @RequestParam Long examId, 
            @RequestParam Long collegeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            User admin = getCurrentAdmin();
            if (admin == null) {
                return ResponseEntity.status(403).body(Map.of("error", "Strictly restricted to University Admin"));
            }
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            org.springframework.data.domain.Page<com.example.examauth.student_exam.model.ExamSeatAllocation> seats = 
                seatAllocationRepository.findAllByExamIdAndCollegeIdOrderBySeatNumber(examId, collegeId, pageable);
                
            org.springframework.data.domain.Page<SeatAllocationDTO> dtoPage = seats.map(s -> 
                new SeatAllocationDTO(s.getId(), s.getRegistrationId(), s.getPrn(), s.getStudentName(), s.getHallName(), s.getSeatNumber(), s.getRollNumber())
            );
            return ResponseEntity.ok(dtoPage);
        } catch (Exception e) {
            log.error("Failed to fetch seats: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
