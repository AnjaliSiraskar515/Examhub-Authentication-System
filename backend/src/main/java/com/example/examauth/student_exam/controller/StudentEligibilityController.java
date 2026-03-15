package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.EligibilityCheckResponseDTO;
import com.example.examauth.student_exam.university.service.ExamEligibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Student-facing REST Controller for exam eligibility check
 * Endpoint: GET /api/student/check-exam-eligibility
 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@Slf4j
public class StudentEligibilityController {

    private final ExamEligibilityService examEligibilityService;

    /**
     * Check if student is eligible for exam session (student-facing endpoint)
     * 
     * @param prnNumber   PRN number of the student
     * @param examSession Exam session to check eligibility for
     * @return EligibilityCheckResponseDTO with eligibility status
     */
    @GetMapping("/check-exam-eligibility")
    public ResponseEntity<EligibilityCheckResponseDTO> checkExamEligibility(
            @RequestParam("prnNumber") String prnNumber,
            @RequestParam("examSession") String examSession) {

        try {
            log.info("Student checking eligibility for PRN: {} and exam session: {}", prnNumber, examSession);

            EligibilityCheckResponseDTO response = examEligibilityService.checkEligibility(prnNumber, examSession);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking exam eligibility: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EligibilityCheckResponseDTO(false, "Server error: " + e.getMessage()));
        }
    }

    /**
     * Test endpoint to verify student controller is working
     * 
     * @return Test message
     */
    @GetMapping("/test-eligibility")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("StudentEligibilityController Working");
    }
}
