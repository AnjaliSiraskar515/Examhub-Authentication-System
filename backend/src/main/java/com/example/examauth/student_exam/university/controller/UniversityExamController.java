package com.example.examauth.student_exam.university.controller;

import com.example.examauth.student_exam.university.model.UniversityExam;
import com.example.examauth.student_exam.university.service.UniversityExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.examauth.repo.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/university/exams")
@RequiredArgsConstructor
@CrossOrigin(allowedHeaders = "*", originPatterns = "*", allowCredentials = "true")
public class UniversityExamController {

    private final UniversityExamService service;
    private final UserRepository userRepository;

    private String getCurrentAdminInstitutionCode() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) return null;
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String roleStr = SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        String role = roleStr.replace("ROLE_", "");
        com.example.examauth.model.User admin = userRepository.findFirstByEmailAndRole(email, role).orElse(null);
        if (admin != null && "UNIVERSITY_ADMIN".equals(admin.getRole())) {
            String c = admin.getInstitutionCode(); return c != null ? c.trim() : null;
        }
        return null;
    }

    @PostMapping({ "", "/" })
    public ResponseEntity<?> createExam(@RequestBody UniversityExam request) {
        if (request.getSubjectIds() == null || request.getSubjectIds().isEmpty()) {
            return ResponseEntity.badRequest().body("Subject IDs are mandatory for exam creation.");
        }
        
        String myInstCode = getCurrentAdminInstitutionCode();
        if (myInstCode != null) {
            request.setInstitutionCode(myInstCode);
        }

        return ResponseEntity.ok(service.createExam(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UniversityExam> updateExam(@PathVariable Long id, @RequestBody UniversityExam request) {
        return ResponseEntity.ok(service.updateExam(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<UniversityExam> publishExam(@PathVariable Long id) {
        return ResponseEntity.ok(service.publishExam(id));
    }

    @GetMapping({ "", "/" })
    public ResponseEntity<List<UniversityExam>> getAllExams() {
        String myInstCode = getCurrentAdminInstitutionCode();
        List<UniversityExam> all = service.getAllExams();
        
        System.out.println("====== EXAM DEBUG ======");
        System.out.println("My Inst Code: " + myInstCode);
        System.out.println("Total Exams in DB: " + all.size());

        if (myInstCode != null && !myInstCode.isEmpty()) {
            all = all.stream()
                .filter(e -> myInstCode.equalsIgnoreCase(e.getInstitutionCode()) 
                          || e.getInstitutionCode() == null 
                          || e.getInstitutionCode().trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());
        }
        
        System.out.println("Returning Exams: " + all.size());
        
        return ResponseEntity.ok(all);
    }

    @GetMapping("/debug-all")
    public ResponseEntity<List<UniversityExam>> getDebugAllExams() {
        return ResponseEntity.ok(service.getAllExams());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UniversityExam> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getExamById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        service.deleteExam(id);
        return ResponseEntity.noContent().build();
    }
}
