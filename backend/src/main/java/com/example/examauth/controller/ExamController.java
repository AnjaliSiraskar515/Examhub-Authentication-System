package com.example.examauth.controller;

import com.example.examauth.model.Exam;
import com.example.examauth.service.ExamService;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import com.example.examauth.student_exam.university.model.UniversityExam;
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

    @Autowired
    private UniversityExamRepository universityExamRepository;

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

    @GetMapping("/all")
    public ResponseEntity<?> getAllExams() {
        return ResponseEntity.ok(examService.getAllExams());
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

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateExamStatus(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (id.startsWith("LEGACY_")) {
                Long examId = Long.parseLong(id.substring(7));
                Optional<Exam> optExam = examService.getExamById(examId);
                if (optExam.isPresent()) {
                    Exam exam = optExam.get();
                    exam.setStatus(status);
                    examService.createExam(exam);
                    return ResponseEntity.ok(Map.of("message", "Legacy exam status updated"));
                }
            } else if (id.startsWith("UNIV_")) {
                Long univId = Long.parseLong(id.substring(5));
                Optional<UniversityExam> optExam = universityExamRepository.findById(univId);
                if (optExam.isPresent()) {
                    UniversityExam exam = optExam.get();
                    exam.setStatus(status);
                    universityExamRepository.save(exam);
                    return ResponseEntity.ok(Map.of("message", "University exam status updated"));
                }
            }
            return ResponseEntity.status(404).body(Map.of("error", "Exam not found or invalid ID format"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update exam status"));
        }
    }
}
