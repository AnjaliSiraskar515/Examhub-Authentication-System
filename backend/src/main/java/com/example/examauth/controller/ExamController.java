package com.example.examauth.controller;

import com.example.examauth.model.Exam;
import com.example.examauth.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @PostMapping("/create")
    public ResponseEntity<?> createExam(@RequestBody Map<String, Object> request) {
        try {
            Exam exam = new Exam();
            exam.setExamName((String) request.get("examName"));
            exam.setInstitutionName((String) request.get("institutionName"));
            exam.setDate(java.time.LocalDate.parse((String) request.get("date")));
            exam.setStartTime(java.time.LocalTime.parse((String) request.get("startTime")));
            exam.setDurationMinutes((Integer) request.get("durationMinutes"));
            exam.setMode((String) request.get("mode"));
            exam.setLocation((String) request.get("location"));
            exam.setStatus("upcoming");
            Exam savedExam = examService.createExam(exam);
            return ResponseEntity.ok(Map.of("message", "Exam created successfully", "examId", savedExam.getExamId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create exam"));
        }
    }

    @GetMapping("/list/{institutionName}")
    public ResponseEntity<?> getExamsByInstitution(@PathVariable String institutionName) {
        List<Exam> exams = examService.getExamsByInstitution(institutionName);
        return ResponseEntity.ok(exams);
    }

    @GetMapping("/student/{userId}")
    public ResponseEntity<?> getStudentExams(@PathVariable Long userId) {
        // For now, return all exams; later filter by assigned students
        List<Exam> exams = examService.getAllExams();
        return ResponseEntity.ok(exams);
    }

    @PostMapping("/{examId}/assign-supervisor")
    public ResponseEntity<?> assignSupervisor(@PathVariable Long examId, @RequestBody Map<String, Long> request) {
        try {
            // Mock implementation for demo
            return ResponseEntity.ok(Map.of("message", "Supervisor assigned successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to assign supervisor"));
        }
    }
}
