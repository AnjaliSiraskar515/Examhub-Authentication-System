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

    @GetMapping("/eligible")
    public ResponseEntity<?> getEligibleExams() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // assuming PRN or username

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User student = userOpt.get();
        List<Exam> exams = examEligibilityService.getEligibleExams(student);
        return ResponseEntity.ok(exams);
    }
}
