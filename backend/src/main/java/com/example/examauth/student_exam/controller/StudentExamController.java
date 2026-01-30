package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.ExamResponseDTO;
import com.example.examauth.student_exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/exams")
@RequiredArgsConstructor
public class StudentExamController {

    private final ExamService examService;

    @GetMapping
    public List<ExamResponseDTO> getAllExams() {
        return examService.getAllExams();
    }
}
