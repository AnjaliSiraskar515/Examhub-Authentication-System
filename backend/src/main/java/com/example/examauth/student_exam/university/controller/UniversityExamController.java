package com.example.examauth.student_exam.university.controller;

import com.example.examauth.student_exam.university.model.UniversityExam;
import com.example.examauth.student_exam.university.service.UniversityExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/university/exams")
@RequiredArgsConstructor
@CrossOrigin(allowedHeaders = "*", originPatterns = "*", allowCredentials = "true")
public class UniversityExamController {

    private final UniversityExamService service;

    @PostMapping({ "", "/" })
    public ResponseEntity<?> createExam(@RequestBody UniversityExam request) {
        if (request.getSubjectIds() == null || request.getSubjectIds().isEmpty()) {
            return ResponseEntity.badRequest().body("Subject IDs are mandatory for exam creation.");
        }
        return ResponseEntity.ok(service.createExam(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UniversityExam> updateExam(@PathVariable Long id, @RequestBody UniversityExam request) {
        return ResponseEntity.ok(service.updateExam(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<UniversityExam> publishExam(@PathVariable Long id) {
        return ResponseEntity.ok(service.publishExam(id));
    }

    @GetMapping({ "", "/" })
    public ResponseEntity<List<UniversityExam>> getAllExams() {
        return ResponseEntity.ok(service.getAllExams());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UniversityExam> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getExamById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        service.deleteExam(id);
        return ResponseEntity.noContent().build();
    }
}
