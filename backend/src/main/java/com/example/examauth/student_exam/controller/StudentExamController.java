package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.ExamResponseDTO;
import com.example.examauth.student_exam.service.StudentExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@CrossOrigin(allowedHeaders = "*", originPatterns = "*", allowCredentials = "true")
@RequestMapping("/api/student/exams")
@RequiredArgsConstructor
public class StudentExamController {

    private final StudentExamService studentExamService;

    @GetMapping
    public ResponseEntity<List<ExamResponseDTO>> getAllExams() {
        return ResponseEntity.ok(studentExamService.getAllExams());
    }
}
