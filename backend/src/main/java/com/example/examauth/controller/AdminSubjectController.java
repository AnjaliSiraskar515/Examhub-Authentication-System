package com.example.examauth.controller;

import com.example.examauth.model.Department;
import com.example.examauth.model.Subject;
import com.example.examauth.repo.DepartmentRepository;
import com.example.examauth.repo.SubjectRepository;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.service.CsvProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.examauth.model.StudentBacklog;
import com.example.examauth.repo.StudentBacklogRepository;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Subject>> getSubjects(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) Integer semester) {

        // --- University isolation ---
        String myUniv = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String email = auth.getName();
            String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            var adminOpt = userRepository.findFirstByEmailAndRole(email, role);
            if (adminOpt.isPresent() && "UNIVERSITY_ADMIN".equals(adminOpt.get().getRole())) {
                String __n = adminOpt.get().getUniversityName(); myUniv = __n != null ? __n.trim() : null;
            }
        }
        final String scopedUniv = myUniv;
        final boolean isUnivAdmin = scopedUniv != null;
        // ----------------------------

        boolean hasCourse = course != null && !course.isBlank();
        boolean hasDept   = departmentId != null;
        boolean hasSem    = semester != null;

        List<Subject> subjects;
        if (hasCourse && hasDept && hasSem) {
            subjects = subjectRepository.findByCourseAndDepartmentEntityIdAndSemester(course, departmentId, semester);
        } else if (hasCourse && hasDept) {
            subjects = subjectRepository.findByCourseAndDepartmentEntityId(course, departmentId);
        } else if (hasCourse && hasSem) {
            subjects = subjectRepository.findByCourseAndSemester(course, semester);
        } else if (hasDept && hasSem) {
            subjects = subjectRepository.findByDepartmentEntityIdAndSemester(departmentId, semester);
        } else if (hasCourse) {
            subjects = subjectRepository.findByCourse(course);
        } else if (hasDept) {
            subjects = subjectRepository.findByDepartmentEntityId(departmentId);
        } else if (hasSem) {
            subjects = subjectRepository.findBySemester(semester);
        } else {
            subjects = subjectRepository.findAll();
        }

        // Subjects are university-wide: university admin sees all subjects.
        // No college-level isolation needed — subjects belong to the university.

        // Apply departmentName filter after collection (cross-college name matching)
        if (departmentName != null && !departmentName.isBlank()) {
            final String deptNameFinal = departmentName.trim();
            subjects = subjects.stream().filter(s ->
                s.getDepartmentEntity() != null &&
                deptNameFinal.equalsIgnoreCase(s.getDepartmentEntity().getName())
            ).collect(Collectors.toList());
        }

        return ResponseEntity.ok(subjects);
    }

    @GetMapping("/backlogged")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Subject>> getBackloggedSubjects(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) Integer semester) {

        // Build initial subject list using available filters.
        // For backlogs, we DO NOT filter the initial Subject fetch by semester.
        // A subject's DB semester might differ from the semester recorded in StudentBacklog 
        // (especially if the subject was linked via CSV import fallback logic).
        List<Subject> subjects;
        boolean hasCourse = course != null && !course.isBlank();
        boolean hasDeptId = departmentId != null;
        boolean hasSem    = semester != null;

        if (hasCourse && hasDeptId) {
            subjects = subjectRepository.findByCourseAndDepartmentEntityId(course, departmentId);
        } else if (hasCourse) {
            subjects = subjectRepository.findByCourse(course);
        } else if (hasDeptId) {
            subjects = subjectRepository.findByDepartmentEntityId(departmentId);
        } else {
            subjects = subjectRepository.findAll();
        }

        // Apply departmentName filter (same cross-college name matching as main endpoint)
        if (departmentName != null && !departmentName.isBlank()) {
            final String deptNameFinal = departmentName.trim();
            subjects = subjects.stream().filter(s ->
                s.getDepartmentEntity() != null &&
                deptNameFinal.equalsIgnoreCase(s.getDepartmentEntity().getName())
            ).collect(Collectors.toList());
        }

        // Restore intelligent filter: only include subjects with at least one active (uncleared) student backlog
        // that MATCHES the requested semester.
        List<Subject> backloggedSubjects = new ArrayList<>();
        String targetSem = hasSem ? String.valueOf(semester) : null;

        for (Subject subject : subjects) {
            // Primary lookup by FK
            List<StudentBacklog> byId = studentBacklogRepository.findBySubjectId(subject.getId());
            boolean hasActive = byId.stream().anyMatch(b -> 
                (b.getCleared() == null || !b.getCleared()) &&
                (targetSem == null || targetSem.equals(b.getSemester()))
            );

            // Fallback lookup by subject name (handles ID-mismatch from CSV import fallback logic)
            if (!hasActive && subject.getName() != null) {
                List<StudentBacklog> byName = studentBacklogRepository.findBySubjectNameIgnoreCase(subject.getName());
                hasActive = byName.stream().anyMatch(b -> 
                    (b.getCleared() == null || !b.getCleared()) &&
                    (targetSem == null || targetSem.equals(b.getSemester()))
                );
            }

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
