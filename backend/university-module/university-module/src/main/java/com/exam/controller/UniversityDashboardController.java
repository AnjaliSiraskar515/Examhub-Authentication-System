package com.exam.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.exam.repository.ExamRepository;
import com.exam.repository.CollegeRepository;
import com.exam.repository.SupervisorRepository;
import com.exam.repository.ExamAssignmentRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/university")
@CrossOrigin(origins = "*")
public class UniversityDashboardController {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private SupervisorRepository supervisorRepository;

    @Autowired
    private ExamAssignmentRepository assignmentRepository;

    @GetMapping("/dashboard")
    public Map<String, Long> getDashboardStats() {

        Map<String, Long> stats = new HashMap<>();

        stats.put("totalExams", examRepository.count());
        stats.put("totalColleges", collegeRepository.count());
        stats.put("totalSupervisors", supervisorRepository.count());
        stats.put("totalAssignments", assignmentRepository.count());

        return stats;
    }
}
