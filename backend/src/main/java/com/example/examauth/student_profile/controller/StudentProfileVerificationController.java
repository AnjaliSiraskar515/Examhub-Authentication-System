package com.example.examauth.student_profile.controller;

import com.example.examauth.student_profile.dto.FaceVerificationRequestDTO;
import com.example.examauth.student_profile.service.StudentProfileVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student-profile")
public class StudentProfileVerificationController {

    private final StudentProfileVerificationService studentProfileVerificationService;

    public StudentProfileVerificationController(
            StudentProfileVerificationService studentProfileVerificationService) {
        this.studentProfileVerificationService = studentProfileVerificationService;
    }

    /**
     * POST /api/student-profile/verify
     *
     * Accepts a FaceVerificationRequestDTO with profileId, idCardImage (Base64),
     * and liveImage (Base64). Delegates to the verification service which calls
     * the Flask AI, saves the image, and locks the profile on success.
     *
     * @param request the verification request body
     * @return 200 OK with result map, or 404 if profile not found
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyProfile(
            @RequestBody FaceVerificationRequestDTO request) {
        try {
            Map<String, Object> response = studentProfileVerificationService.verifyAndLockProfile(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
