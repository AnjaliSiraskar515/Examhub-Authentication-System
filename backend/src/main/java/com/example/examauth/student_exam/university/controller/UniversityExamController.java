package com.example.examauth.student_exam.university.controller;

import com.example.examauth.student_exam.university.model.UniversityExam;
import com.example.examauth.student_exam.university.service.UniversityExamService;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.repo.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/university/exams")
@RequiredArgsConstructor
@CrossOrigin(allowedHeaders = "*", originPatterns = "*", allowCredentials = "true")
public class UniversityExamController {

    private final UniversityExamService service;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;

    @PostMapping({ "", "/" })
    public ResponseEntity<UniversityExam> createExam(
            @RequestBody UniversityExam request,
            Authentication authentication) {
        // Populate institutionCode from the authenticated university admin
        if (authentication != null && authentication.getName() != null) {
            userRepository.findFirstByEmail(authentication.getName()).ifPresent(user -> {
                String instCode = user.getInstitutionCode();
                if (instCode != null && !instCode.isEmpty()) {
                    request.setInstitutionCode(instCode);
                } else {
                    // Fallback: look up by email in institutions table
                    institutionRepository.findFirstByAdminEmail(user.getEmail())
                            .ifPresent(inst -> request.setInstitutionCode(inst.getInstitutionCode()));
                    if (request.getInstitutionCode() == null) {
                        institutionRepository.findFirstByContactEmail(user.getEmail())
                                .ifPresent(inst -> request.setInstitutionCode(inst.getInstitutionCode()));
                    }
                }
            });
        }
        return ResponseEntity.ok(service.createExam(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UniversityExam> updateExam(
            @PathVariable Long id,
            @RequestBody UniversityExam request,
            Authentication authentication) {
        // Preserve institutionCode if not already set
        if (authentication != null && authentication.getName() != null && request.getInstitutionCode() == null) {
            userRepository.findFirstByEmail(authentication.getName()).ifPresent(user -> {
                String instCode = user.getInstitutionCode();
                if (instCode != null && !instCode.isEmpty()) {
                    request.setInstitutionCode(instCode);
                } else {
                    institutionRepository.findFirstByAdminEmail(user.getEmail())
                            .ifPresent(inst -> request.setInstitutionCode(inst.getInstitutionCode()));
                }
            });
        }
        return ResponseEntity.ok(service.updateExam(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<UniversityExam> publishExam(@PathVariable Long id) {
        return ResponseEntity.ok(service.publishExam(id));
    }

    @GetMapping({ "", "/" })
    public ResponseEntity<List<UniversityExam>> getAllExams() {
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
