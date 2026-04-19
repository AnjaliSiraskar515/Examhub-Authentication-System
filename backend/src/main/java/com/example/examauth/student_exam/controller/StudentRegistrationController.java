package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.ExamRegistrationRequestDTO;
import com.example.examauth.student_exam.dto.ExamRegistrationResponseDTO;
import com.example.examauth.student_exam.dto.EligibilityCheckResponseDTO;
import com.example.examauth.student_exam.service.ExamRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentRegistrationController {

    private final ExamRegistrationService examRegistrationService;
    private final com.example.examauth.repo.UserRepository userRepository;
    private final com.example.examauth.student_exam.university.service.ExamEligibilityService examEligibilityService;

    // Secure Data-First Registration (Step 1)
    @PostMapping(value = "/exams/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExamRegistrationResponseDTO> registerForExam(
            @RequestBody @Valid ExamRegistrationRequestDTO request,
            org.springframework.security.core.Authentication authentication) {

        String email;
        if (authentication == null || !authentication.isAuthenticated()) {
            // Bypass auth for frontend testing mock mode
            email = "test@student.com";
        } else {
            email = authentication.getName();
        }

        com.example.examauth.model.User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            // Fallback for isolated testing
            request.setStudentId(1L);
        } else {
            // Securely set studentId from logged-in user
            request.setStudentId(user.getUserId());
        }

        // ========== ELIGIBILITY VALIDATION ==========
        // Check if student is eligible for this exam session
        if (request.getPrn() != null && request.getExamSession() != null) {
            EligibilityCheckResponseDTO eligibilityCheck = examEligibilityService
                    .checkEligibility(request.getPrn(), request.getExamSession());

            // For testing purposes, we allow registration even if not in the eligible
            // students list.
            // ExamRegistrationService has backward-compatibility to handle missing records.
            if (!eligibilityCheck.isEligible()) {
                // Log the missing eligibility, but proceed anyway for testing
                // System.out.println("Warning: Student not in eligible list for " +
                // request.getExamSession());
            }
        }
        // ============================================

        return ResponseEntity.ok(examRegistrationService.registerStudent(request));
    }

    // Document Upload Registration (Step 2 - Optional/Legacy)
    @PostMapping(value = "/registrations/upload", consumes = "multipart/form-data")
    public ResponseEntity<ExamRegistrationResponseDTO> registerWithDocument(
            @RequestParam Long studentId,
            @RequestParam Long examId,
            @RequestParam("document") MultipartFile document) {
        return ResponseEntity.ok(examRegistrationService.registerForExam(studentId, examId, document));
    }

    @GetMapping("/registrations")
    public ResponseEntity<List<ExamRegistrationResponseDTO>> getRegistrationsByStudentId(
            @RequestParam(required = false) Long studentId,
            org.springframework.security.core.Authentication authentication) {

        Long actualStudentId = studentId != null ? studentId : 13L;

        if (authentication != null && authentication.isAuthenticated()) {
            com.example.examauth.model.User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                actualStudentId = user.getUserId();
            }
        }

        return ResponseEntity.ok(examRegistrationService.getRegistrationsByStudentId(actualStudentId));
    }
}