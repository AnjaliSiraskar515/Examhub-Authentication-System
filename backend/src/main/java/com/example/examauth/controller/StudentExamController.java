package com.example.examauth.controller;

import com.example.examauth.model.Exam;
import com.example.examauth.model.User;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.service.StudentExamEligibilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/student/exams")
public class StudentExamController {

    @Autowired
    private StudentExamEligibilityService examEligibilityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.examauth.util.JwtUtil jwtUtil;

    @GetMapping("/eligible")
    public ResponseEntity<?> getEligibleExams(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }
        
        String jwt = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(jwt);
        
        Optional<User> userOpt;
        if (userId != null) {
            userOpt = userRepository.findById(userId);
        } else {
            String email = jwtUtil.extractUsername(jwt);
            String role = jwtUtil.extractRole(jwt);
            userOpt = userRepository.findFirstByEmailAndRole(email, role);
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User student = userOpt.get();
        List<Exam> exams = examEligibilityService.getEligibleExams(student);
        return ResponseEntity.ok(exams);
    }
}
