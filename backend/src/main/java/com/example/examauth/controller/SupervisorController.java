package com.example.examauth.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/supervisor")
@CrossOrigin // Allow frontend access
public class SupervisorController {

    private final com.example.examauth.repo.UserRepository userRepository;
    private final com.example.examauth.repo.QrRepository qrRepository;
    private final com.example.examauth.repo.ExamRepository examRepository;

    public SupervisorController(com.example.examauth.repo.UserRepository userRepository,
            com.example.examauth.repo.QrRepository qrRepository,
            com.example.examauth.repo.ExamRepository examRepository) {
        this.userRepository = userRepository;
        this.qrRepository = qrRepository;
        this.examRepository = examRepository;
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile() {
        // Fetch real counts
        long studentsCount = qrRepository.countDistinctStudentIdByUsed(true); // Count verified students
        long scansCount = qrRepository.countByUsed(true);

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "Dr. Supervisor");
        profile.put("email", "supervisor@examhub.edu");
        profile.put("avatar", "https://ui-avatars.com/api/?name=Supervisor&background=random");
        profile.put("assignedExamsCount", examRepository.count()); // Real exam count
        profile.put("studentsCount", studentsCount); // Real count
        profile.put("scansToday", scansCount); // Real count
        profile.put("documents", List.of(Map.of("filename", "Supervisor_Guidelines.pdf")));
        return profile;
    }

    @GetMapping("/exams")
    public List<Map<String, Object>> getExams() {
        return examRepository.findAll().stream().map(exam -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", exam.getExamId());
            map.put("title", exam.getExamName()); // Corrected field name
            map.put("date", exam.getDate().toString()); // Corrected field name
            map.put("duration", exam.getDurationMinutes()); // Corrected field name
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/students")
    public List<Map<String, Object>> getStudents() {
        return userRepository.findByRole("STUDENT").stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getUserId());
            map.put("name", user.getName());
            map.put("email", user.getEmail());
            map.put("regno", user.getUsername()); // Using username as reg no fallback
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }
}
