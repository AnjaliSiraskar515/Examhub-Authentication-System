package com.example.examauth.student_exam.university.controller;

import com.example.examauth.student_exam.dto.UploadResponseDTO;
import com.example.examauth.student_exam.dto.EligibilityCheckResponseDTO;
import com.example.examauth.student_exam.university.service.ExamEligibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for exam eligibility Excel upload
 * Endpoint: POST /api/university/upload-exam-eligibility
 */
@RestController
@RequestMapping("/api/university")
@RequiredArgsConstructor
@Slf4j
public class ExamEligibilityController {

    private final ExamEligibilityService examEligibilityService;

    /**
     * Upload Excel file with eligible students for exam session
     * 
     * @param file         Excel file (.xlsx format)
     * @param universityId Optional university ID (defaults to 1)
     * @return UploadResponseDTO with statistics
     */
    @PostMapping("/upload-exam-eligibility")
    public ResponseEntity<UploadResponseDTO> uploadExamEligibility(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "universityId", required = false, defaultValue = "1") Integer universityId) {

        try {
            log.info("Received Excel upload request for university ID: {}", universityId);
            log.info("File name: {}, Size: {} bytes", file.getOriginalFilename(), file.getSize());

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new UploadResponseDTO(0, 0, 0, false, "File is empty"));
            }

            // Check file size (5MB limit)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(new UploadResponseDTO(0, 0, 0, false, "File size exceeds 5MB limit"));
            }

            // Process Excel file
            UploadResponseDTO response = examEligibilityService.processExcelFile(file, universityId);

            if (response.isSuccess()) {
                log.info("Excel upload successful: {}", response.getMessage());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Excel upload failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error processing Excel upload: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadResponseDTO(0, 0, 0, false,
                            "Server error: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint for exam eligibility module
     * 
     * @return Simple status message
     */
    @GetMapping("/exam-eligibility/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Exam Eligibility Module is running");
    }

    /**
     * Test endpoint to verify controller is working
     * 
     * @return Test message
     */
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("ExamEligibilityController Working");
    }

    /**
     * Check if student is eligible for exam session
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
            log.info("Checking eligibility for PRN: {} and exam session: {}", prnNumber, examSession);

            EligibilityCheckResponseDTO response = examEligibilityService
                    .checkEligibility(prnNumber, examSession);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking exam eligibility: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EligibilityCheckResponseDTO(
                            false, "Server error: " + e.getMessage()));
        }
    }
}
