package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.ExamRegistrationRequestDTO;
import com.example.examauth.student_exam.dto.ExamRegistrationResponseDTO;
import com.example.examauth.student_exam.service.ExamRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/student/registrations")
@RequiredArgsConstructor
public class StudentRegistrationController {

    private final ExamRegistrationService examRegistrationService;
    private final com.example.examauth.repo.UserRepository userRepository;
    private final com.example.examauth.student_exam.university.service.ExamEligibilityService examEligibilityService;

    // Secure Data-First Registration (Step 1)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExamRegistrationResponseDTO> registerForExam(
            @RequestBody @Valid ExamRegistrationRequestDTO request,
            org.springframework.security.core.Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        com.example.examauth.model.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Securely set studentId from logged-in user
        request.setStudentId(user.getUserId());

        // ========== ELIGIBILITY VALIDATION ==========
        // Check if student is eligible for this exam session
        if (request.getPrn() != null && request.getExamSession() != null) {
            com.example.examauth.student_exam.university.dto.EligibilityCheckResponseDTO eligibilityCheck = examEligibilityService
                    .checkEligibility(request.getPrn(), request.getExamSession());

            if (!eligibilityCheck.isEligible()) {
                return ResponseEntity
                        .status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(null); // Could return error DTO with message: eligibilityCheck.getMessage()
            }
        }
        // ============================================

        return ResponseEntity.ok(examRegistrationService.registerStudent(request));
    }

    // Document Upload Registration (Step 2 - Optional/Legacy)
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ExamRegistrationResponseDTO> registerWithDocument(
            @RequestParam Long studentId,
            @RequestParam Long examId,
            @RequestParam("document") MultipartFile document) {
        return ResponseEntity.ok(examRegistrationService.registerForExam(studentId, examId, document));
    }

    @GetMapping
    public ResponseEntity<List<ExamRegistrationResponseDTO>> getRegistrationsByStudentId(@RequestParam Long studentId) {
        return ResponseEntity.ok(examRegistrationService.getRegistrationsByStudentId(studentId));
    }
}