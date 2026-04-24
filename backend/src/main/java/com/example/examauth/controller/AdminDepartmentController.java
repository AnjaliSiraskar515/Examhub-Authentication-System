package com.example.examauth.controller;

import com.example.examauth.model.Department;
import com.example.examauth.repo.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
public class AdminDepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;

    @GetMapping
    public ResponseEntity<List<Department>> getDepartments(@RequestParam(required = false) Long collegeId) {
        if (collegeId != null) {
            return ResponseEntity.ok(departmentRepository.findByCollegeId(collegeId));
        }
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestParam Long collegeId, @RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Department name is required");
        }

        java.util.Optional<Department> existing = departmentRepository.findByNameIgnoreCaseAndCollegeId(name.trim(),
                collegeId);
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body("Department already exists in this college.");
        }

        com.example.examauth.model.College college = new com.example.examauth.model.College();
        college.setId(collegeId);

        Department newDept = new Department();
        newDept.setName(name.trim());
        newDept.setCollege(college);

        return ResponseEntity.ok(departmentRepository.save(newDept));
    }
}
