package com.example.examauth.controller;

import com.example.examauth.model.User;
import com.example.examauth.model.Exam;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.service.ExamService;
import com.example.examauth.dto.ExamResponseDTO;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import com.example.examauth.student_exam.university.model.UniversityExam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Autowired
    private UniversityExamRepository universityExamRepository;

    // GET /api/university/{id}/students
    @GetMapping("/{id}/students")
    public ResponseEntity<?> getStudents(@PathVariable Long id) {
        // In a real app, filter by university ID. For now returning all students.
        List<Map<String, Object>> students = userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .map(u -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("userId", u.getUserId());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("major", u.getMajor());
                    map.put("year", u.getYear());
                    map.put("department", u.getDepartment());
                    map.put("status", u.getStatus());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    // GET /api/university/{id}/staff
    @GetMapping("/{id}/staff")
    public ResponseEntity<?> getStaff(@PathVariable Long id) {
        // Returning supervisors
        List<Map<String, Object>> staff = userRepository.findAll().stream()
                .filter(u -> "SUPERVISOR".equalsIgnoreCase(u.getRole()))
                .map(u -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("userId", u.getUserId());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("status", u.getStatus());
                    map.put("department", u.getDepartment());
                    map.put("role", u.getRole());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(staff);
    }

    // POST /api/university/{id}/exam
    @PostMapping("/{id}/exam")
    public ResponseEntity<?> createExam(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Exam exam = new Exam();
            exam.setExamName((String) request.get("examName"));
            // Use provided institution or default
            exam.setInstitutionName((String) request.getOrDefault("institutionName", "University"));

            exam.setDate(java.time.LocalDate.parse((String) request.get("date")));
            exam.setStartTime(java.time.LocalTime.parse((String) request.get("startTime")));

            // Handle potentially different number types from JSON
            Object durationObj = request.get("durationMinutes");
            if (durationObj instanceof Number) {
                exam.setDurationMinutes(((Number) durationObj).intValue());
            } else {
                exam.setDurationMinutes(Integer.parseInt(String.valueOf(durationObj)));
            }

            exam.setMode((String) request.get("mode"));
            exam.setLocation((String) request.get("location"));
            exam.setStatus("upcoming");

            if (request.containsKey("supervisorId") && request.get("supervisorId") != null) {
                exam.setSupervisorId(Long.valueOf(request.get("supervisorId").toString()));
            }
            if (request.containsKey("supervisorName") && request.get("supervisorName") != null) {
                exam.setSupervisorName((String) request.get("supervisorName"));
            }

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
        List<ExamResponseDTO> unifiedExams = new ArrayList<>();

        // 1. Fetch Legacy Exams
        List<Exam> legacyExams = examService.getAllExams();
        for (Exam e : legacyExams) {
            unifiedExams.add(ExamResponseDTO.builder()
                    .sourceId("LEGACY_" + e.getExamId())
                    .id(e.getExamId())
                    .source("LEGACY")
                    .examName(e.getExamName())
                    .date(e.getDate())
                    .startTime(e.getStartTime())
                    .durationMinutes(e.getDurationMinutes() != null ? e.getDurationMinutes() : 0)
                    .status(e.getStatus())
                    .mode(e.getMode())
                    .location(e.getLocation() != null ? e.getLocation() : "N/A")
                    .build());
        }

        // 2. Fetch University Exams (New)
        List<UniversityExam> universityExams = universityExamRepository.findAll();
        for (UniversityExam ue : universityExams) {
            unifiedExams.add(ExamResponseDTO.builder()
                    .sourceId("UNIV_" + ue.getId())
                    .id(ue.getId())
                    .source("UNIVERSITY")
                    .examName(ue.getSessionName())
                    .date(ue.getSchedule() != null ? ue.getSchedule().getExamDate() : null)
                    .startTime(ue.getSchedule() != null ? ue.getSchedule().getStartTime() : null)
                    .durationMinutes(0) // Default as per requirement
                    .status(ue.getStatus())
                    .mode(ue.getMode())
                    .location(ue.getCenterName() != null ? ue.getCenterName() : "N/A")
                    .build());
        }

        // 3. Sort by Date + Time (Latest first)
        unifiedExams.sort(Comparator.comparing((ExamResponseDTO e) -> {
            if (e.getDate() == null)
                return LocalDateTime.MIN;
            return LocalDateTime.of(e.getDate(), e.getStartTime() != null ? e.getStartTime() : java.time.LocalTime.MIN);
        }).reversed());

        return ResponseEntity.ok(unifiedExams);
    }

    @DeleteMapping("/{univId}/exam/{id}")
    public ResponseEntity<?> deleteExam(@PathVariable Long univId, @PathVariable String id) {
        try {
            if (id.startsWith("LEGACY_")) {
                Long examId = Long.parseLong(id.substring(7));
                examService.deleteExam(examId);
                return ResponseEntity.ok(Map.of("message", "Legacy exam deleted"));
            } else if (id.startsWith("UNIV_")) {
                Long targetUnivId = Long.parseLong(id.substring(5));
                universityExamRepository.deleteById(targetUnivId);
                return ResponseEntity.ok(Map.of("message", "University exam deleted"));
            }
            return ResponseEntity.status(404).body(Map.of("error", "Exam not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete exam: " + e.getMessage()));
        }
    }
}
