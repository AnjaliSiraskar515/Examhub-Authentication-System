package com.example.examauth.student_profile.controller;

import com.example.examauth.student_profile.dto.FaceVerificationRequestDTO;
import com.example.examauth.student_profile.service.StudentProfileVerificationService;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.util.HashUtil;
import com.example.examauth.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/student-profile")
public class StudentProfileVerificationController {

    private final StudentProfileVerificationService studentProfileVerificationService;
    private final UserRepository userRepository;

    public StudentProfileVerificationController(
            StudentProfileVerificationService studentProfileVerificationService,
            UserRepository userRepository) {
        this.studentProfileVerificationService = studentProfileVerificationService;
        this.userRepository = userRepository;
    }

    private Map<String, Object> createResponse(boolean success, String message) {
        return Map.of(
                "success", success,
                "message", message,
                "timestamp", OffsetDateTime.now().toString());
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

    @GetMapping("/biometric/status")
    public ResponseEntity<Map<String, Object>> getBiometricStatus(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createResponse(false, "Unauthorized"));
        }

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));
        if (!isStudent) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createResponse(false, "Access Denied: Only students can perform this action."));
        }

        Optional<User> userOpt = userRepository.findFirstByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createResponse(false, "User not found"));
        }
        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "enrolled", user.isBiometricEnrolled(),
                "timestamp", OffsetDateTime.now().toString()));
    }

    @PostMapping("/biometric/enroll")
    public ResponseEntity<Map<String, Object>> enrollBiometric(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createResponse(false, "Unauthorized"));
        }

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));
        if (!isStudent) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createResponse(false, "Access Denied: Only students can perform this action."));
        }

        String fingerprint = request.get("fingerprint");
        if (fingerprint == null || fingerprint.isEmpty()) {
            return ResponseEntity.badRequest().body(createResponse(false, "Fingerprint data missing"));
        }

        Optional<User> userOpt = userRepository.findFirstByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createResponse(false, "User not found"));
        }

        User user = userOpt.get();
        String hash = HashUtil.sha256(fingerprint + "_SECURE_SALT");
        user.setBiometricTemplateHash(hash);
        user.setBiometricEnrolled(true);
        user.setBiometricEnrolledAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(createResponse(true, "Biometric enrolled successfully."));
    }

    @PostMapping("/biometric/verify")
    public ResponseEntity<Map<String, Object>> verifyBiometric(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createResponse(false, "Unauthorized"));
        }

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));
        if (!isStudent) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createResponse(false, "Access Denied: Only students can perform this action."));
        }

        String fingerprint = request.get("fingerprint");
        if (fingerprint == null || fingerprint.isEmpty()) {
            return ResponseEntity.badRequest().body(createResponse(false, "Fingerprint data missing"));
        }

        Optional<User> userOpt = userRepository.findFirstByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createResponse(false, "User not found"));
        }

        User user = userOpt.get();
        if (!user.isBiometricEnrolled() || user.getBiometricTemplateHash() == null) {
            return ResponseEntity.badRequest()
                    .body(createResponse(false, "Biometric not enrolled. Please enroll first."));
        }

        String hash = HashUtil.sha256(fingerprint + "_SECURE_SALT");
        if (user.getBiometricTemplateHash().equals(hash)) {
            user.setBiometricLastVerified(LocalDateTime.now());
            userRepository.save(user);
            return ResponseEntity.ok(createResponse(true, "Biometric verified successfully."));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createResponse(false, "Biometric verification failed: Fingerprint mismatch."));
    }
}
