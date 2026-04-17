package com.exam.controller;

import com.exam.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private SupervisorRepository supervisorRepository;

    @Autowired
    private ExamAssignmentRepository assignmentRepository;

    @GetMapping
public Map<String, Long> getDashboardStats() {

    Map<String, Long> stats = new HashMap<>();

    stats.put("totalExams", examRepository.count());
    stats.put("totalColleges", collegeRepository.count());
    stats.put("totalSupervisors", supervisorRepository.count());
    stats.put("totalAssignments", assignmentRepository.count());

    stats.put("completedAssignments", 
        assignmentRepository.countByStatus("Completed"));

    stats.put("assignedAssignments", 
        assignmentRepository.countByStatus("Assigned"));

    stats.put("availableSupervisors", 
        supervisorRepository.countByAvailability("Available"));

    stats.put("busySupervisors", 
        supervisorRepository.countByAvailability("Assigned"));

    return stats;
}
}
