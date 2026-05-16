package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.StudentStatsDTO;
import com.example.examauth.student_exam.service.ExamRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@CrossOrigin(allowedHeaders = "*", originPatterns = "*", allowCredentials = "true")
public class StudentStatsController {

    private final ExamRegistrationService examRegistrationService;
    private final com.example.examauth.repo.UserRepository userRepository;

    @GetMapping("/stats")
    public ResponseEntity<StudentStatsDTO> getStudentStats(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        com.example.examauth.model.User user = userRepository.findFirstByEmailAndRole(email, authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""))
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudentStatsDTO stats = examRegistrationService.getStudentStats(user.getUserId());
        return ResponseEntity.ok(stats);
    }
}
