package com.example.examauth.student_profile.controller;

import com.example.examauth.student_profile.model.StudentProfile;
import com.example.examauth.student_profile.service.StudentProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for the Student Profile module.
 *
 * Base URL: /api/student-profile
 *
 * NOTE: This controller uses /api/student-profile (NOT /api/profile)
 * to avoid conflicts with the existing ProfileController.
 */
@RestController
@RequestMapping("/api/student-profile")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    /**
     * GET /api/student-profile/{id}
     * Returns the full profile details for the given ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        Optional<StudentProfile> profile = studentProfileService.getProfileById(id);
        if (profile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Student profile not found with id: " + id));
        }
        return ResponseEntity.ok(profile.get());
    }

    /**
     * PUT /api/student-profile/{id}/year
     * Updates the 'year' field of the student profile.
     * Returns 423 Locked if the profile is locked.
     *
     * Request body: { "year": "Third Year" }
     */
    @PutMapping("/{id}/year")
    public ResponseEntity<?> updateYear(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String newYear = body.get("year");
        if (newYear == null || newYear.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Request body must contain a non-empty 'year' field."));
        }

        try {
            StudentProfile updated = studentProfileService.updateYear(id, newYear);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            // 423 Locked – profile is locked, year update not allowed
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
