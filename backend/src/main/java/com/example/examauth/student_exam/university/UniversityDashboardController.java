package com.example.examauth.student_exam.university;

import com.example.examauth.student_exam.model.ExamRegistration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/university")
@RequiredArgsConstructor
@CrossOrigin(allowedHeaders = "*", originPatterns = "*", allowCredentials = "true") // Allow frontend access
public class UniversityDashboardController {

    private final UniversityService universityService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(universityService.getDashboardStats());
    }

    @GetMapping("/{universityId}/registrations")
    public ResponseEntity<List<ExamRegistration>> getAllRegistrations(@PathVariable Long universityId) {
        return ResponseEntity.ok(universityService.getAllRegistrations());
    }

    @PostMapping("/registrations/{id}/approve")
    public ResponseEntity<?> approveRegistration(@PathVariable Long id) {
        universityService.approveRegistration(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/registrations/{id}/reject")
    public ResponseEntity<?> rejectRegistration(@PathVariable Long id) {
        universityService.rejectRegistration(id);
        return ResponseEntity.ok().build();
    }
}
