package com.example.examauth.controller;

import com.example.examauth.model.User;
import com.example.examauth.model.Exam;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/university")
public class UniversityController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamService examService;

    // GET /api/university/{id}/students
    @GetMapping("/{id}/students")
    public ResponseEntity<?> getStudents(@PathVariable Long id) {
        // In a real app, filter by university ID. For now returning all students as per
        // demo requirement.
        List<User> students = userRepository.findAll().stream()
                .filter(u -> "student".equalsIgnoreCase(u.getRole()) || "STUDENT".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    // GET /api/university/{id}/staff
    @GetMapping("/{id}/staff")
    public ResponseEntity<?> getStaff(@PathVariable Long id) {
        // Returning supervisors
        List<User> staff = userRepository.findAll().stream()
                .filter(u -> "supervisor".equalsIgnoreCase(u.getRole()) || "SUPERVISOR".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(staff);
    }

    // POST /api/university/{id}/exam
    @PostMapping("/{id}/exam")
    public ResponseEntity<?> createExam(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Exam exam = new Exam();
            exam.setExamName((String) request.get("examName"));
            // Assuming institutionName comes from frontend or derived from ID.
            // For now hardcoding or using request param if available
            exam.setInstitutionName((String) request.getOrDefault("institutionName", "University " + id));

            exam.setDate(java.time.LocalDate.parse((String) request.get("date")));
            exam.setStartTime(java.time.LocalTime.parse((String) request.get("startTime")));
            exam.setDurationMinutes(Integer.parseInt(String.valueOf(request.get("durationMinutes"))));
            exam.setMode((String) request.get("mode"));
            exam.setLocation((String) request.get("location"));
            exam.setStatus("upcoming");

            Exam savedExam = examService.createExam(exam);
            return ResponseEntity.ok(Map.of("message", "Exam created successfully", "examId", savedExam.getExamId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create exam: " + e.getMessage()));
        }
    }

    // GET /api/university/{id}/exam
    @GetMapping("/{id}/exam")
    public ResponseEntity<?> getExams(@PathVariable Long id) {
        // Fetch exams by institution name. Assuming "University" or deriving name from
        // ID.
        // For demo, we are using "University" or "University 1"
        return ResponseEntity.ok(examService.getExamsByInstitution("University"));
    }
}
