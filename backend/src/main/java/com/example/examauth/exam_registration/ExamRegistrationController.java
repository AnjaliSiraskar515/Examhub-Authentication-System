package com.example.examauth.exam_registration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam-registration")
public class ExamRegistrationController {

    private final ExamRegistrationService examRegistrationService;

    public ExamRegistrationController(ExamRegistrationService examRegistrationService) {
        this.examRegistrationService = examRegistrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        try {
            examRegistrationService.registerForExam(request.getStudentId(), request.getExamId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Registration successful");
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            Map<String, String> error = new HashMap<>();
            error.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ExamRegistration>> getByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(examRegistrationService.getRegistrationsForStudent(studentId));
    }

    // Request body for /register
    public static class RegisterRequest {
        private Long studentId;
        private Long examId;

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public Long getExamId() {
            return examId;
        }

        public void setExamId(Long examId) {
            this.examId = examId;
        }
    }
}
