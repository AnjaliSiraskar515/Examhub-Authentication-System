package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.ExamRegistrationRequestDTO;
import com.example.examauth.student_exam.dto.ExamRegistrationResponseDTO;
import com.example.examauth.student_exam.service.ExamRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class RegistrationValidationTestController {

    private final ExamRegistrationService examRegistrationService;

    @PostMapping("/registration-validation")
    public ResponseEntity<?> testRegistrationValidation() {
        try {
            // Create dummy request
            ExamRegistrationRequestDTO request = new ExamRegistrationRequestDTO();

            // Set required fields from old DTO (to pass @NotNull validation)
            request.setStudentId(101L); // CRUCIAL: Set studentId for notification creation
            request.setExamId(1L);
            request.setPrn("PRN2024001");
            request.setFullName("John Doe");
            request.setCourse("BSc CS");
            request.setYear("First Year");

            // Set fields for validation testing
            request.setExamSession("Winter 2024");
            request.setExamType("REGULAR");
            request.setSelectedSubjects(Collections.singletonList("Mathematics"));
            request.setDeclarationAccepted(true);
            request.setPaymentStatus("PENDING");

            // Attempt registration
            ExamRegistrationResponseDTO response = examRegistrationService.registerStudent(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Validation Failed: " + e.getMessage());
        }
    }
}
