package com.example.examauth.controller;

import com.example.examauth.model.Department;
import com.example.examauth.model.Subject;
import com.example.examauth.repo.DepartmentRepository;
import com.example.examauth.repo.SubjectRepository;
import com.example.examauth.service.CsvProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.examauth.model.StudentBacklog;
import com.example.examauth.repo.StudentBacklogRepository;

import java.util.List;
import java.util.ArrayList;
@RestController
@RequestMapping("/api/admin/subjects")
public class AdminSubjectController {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CsvProcessingService csvProcessingService;

    @Autowired
    private StudentBacklogRepository studentBacklogRepository;

    @GetMapping
    public ResponseEntity<List<Subject>> getSubjects(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer semester) {

        boolean hasCourse = course != null && !course.isBlank();
        boolean hasDept   = departmentId != null;
        boolean hasSem    = semester != null;

        if (hasCourse && hasDept && hasSem) {
            return ResponseEntity.ok(subjectRepository.findByCourseAndDepartmentEntityIdAndSemester(course, departmentId, semester));
        } else if (hasCourse && hasDept) {
            return ResponseEntity.ok(subjectRepository.findByCourseAndDepartmentEntityId(course, departmentId));
        } else if (hasCourse && hasSem) {
            return ResponseEntity.ok(subjectRepository.findByCourseAndSemester(course, semester));
        } else if (hasDept && hasSem) {
            return ResponseEntity.ok(subjectRepository.findByDepartmentEntityIdAndSemester(departmentId, semester));
        } else if (hasCourse) {
            return ResponseEntity.ok(subjectRepository.findByCourse(course));
        } else if (hasDept) {
            return ResponseEntity.ok(subjectRepository.findByDepartmentEntityId(departmentId));
        } else if (hasSem) {
            return ResponseEntity.ok(subjectRepository.findBySemester(semester));
        }

        return ResponseEntity.ok(subjectRepository.findAll());
    }

    @GetMapping("/backlogged")
    public ResponseEntity<List<Subject>> getBackloggedSubjects(
            @RequestParam String course,
            @RequestParam Long departmentId,
            @RequestParam Integer semester) {

        List<Subject> subjects = subjectRepository.findByCourseAndDepartmentEntityIdAndSemester(course, departmentId, semester);
        List<Subject> backloggedSubjects = new ArrayList<>();
        
        for (Subject subject : subjects) {
            List<StudentBacklog> backlogs = studentBacklogRepository.findBySubjectId(subject.getId());
            boolean hasActive = backlogs.stream().anyMatch(b -> b.getCleared() != null && !b.getCleared());
            if (hasActive) {
                backloggedSubjects.add(subject);
            }
        }
        
        return ResponseEntity.ok(backloggedSubjects);
    }

    @PostMapping
    public ResponseEntity<?> createSubject(@RequestBody Subject request) {
        if (request.getDepartmentEntity() == null || request.getDepartmentEntity().getId() == null) {
            return ResponseEntity.badRequest().body("Department ID is mandatory.");
        }
        
        Long deptId = request.getDepartmentEntity().getId();
        Department dept = departmentRepository.findById(deptId).orElse(null);
        if (dept == null) {
            return ResponseEntity.badRequest().body("Invalid Department ID.");
        }
        
        request.setDepartmentEntity(dept);

        // Check uniqueness
        if (subjectRepository.findByCodeAndSemesterAndDepartmentEntityIdAndCourse(request.getCode(), request.getSemester(), deptId, request.getCourse()).isPresent()) {
            return ResponseEntity.badRequest().body("Subject already exists with this code, semester, department and course.");
        }

        Subject saved = subjectRepository.save(request);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSubjectsCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a CSV file!");
        }

        try {
            List<String[]> rows = csvProcessingService.parseCsv(file);
            int processedCount = 0;
            // Expected Format: Code, Name, Semester, DepartmentId, Course
            for (String[] row : rows) {
                if (row.length < 5) continue;
                String code = row[0].trim();
                String name = row[1].trim();
                String semesterStr = row[2].trim();
                String deptIdStr = row[3].trim();
                String course = row[4].trim();

                try {
                    Integer semester = Integer.parseInt(semesterStr);
                    Long deptId = Long.parseLong(deptIdStr);

                    Department dept = departmentRepository.findById(deptId).orElse(null);
                    if (dept == null) continue;

                    if (subjectRepository.findByCodeAndSemesterAndDepartmentEntityIdAndCourse(code, semester, deptId, course).isEmpty()) {
                        Subject newSub = new Subject();
                        newSub.setCode(code);
                        newSub.setName(name);
                        newSub.setSemester(semester);
                        newSub.setDepartmentEntity(dept);
                        newSub.setCourse(course);
                        subjectRepository.save(newSub);
                        processedCount++;
                    }

                } catch (Exception e) {
                    System.out.println("Row failed parsing in subject CSV: " + code);
                }
            }
            return ResponseEntity.ok("Processed " + processedCount + " new subjects successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process CSV file: " + e.getMessage());
        }
    }
}
