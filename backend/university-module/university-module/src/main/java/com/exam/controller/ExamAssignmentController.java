package com.exam.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.exam.entity.ExamAssignment;
import com.exam.repository.ExamAssignmentRepository;
import com.exam.repository.ExamRepository;
import com.exam.repository.CollegeRepository;
import com.exam.repository.SupervisorRepository;

import com.exam.entity.Exam;
import com.exam.entity.College;
import com.exam.entity.Supervisor;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "*")
public class ExamAssignmentController {

    @Autowired
    private ExamAssignmentRepository assignmentRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private SupervisorRepository supervisorRepository;

    @GetMapping
    public List<ExamAssignment> getAll() {
        return assignmentRepository.findAll();
    }

    @PutMapping("/{id}/complete")
public ExamAssignment markCompleted(@PathVariable Long id) {

    ExamAssignment assignment = assignmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Assignment not found"));

    // Update assignment status
    assignment.setStatus("Completed");

    // Free supervisor
    Supervisor supervisor = assignment.getSupervisor();
    supervisor.setAvailability("Available");
    supervisorRepository.save(supervisor);

    return assignmentRepository.save(assignment);
}

@PostMapping("/auto")
public ExamAssignment autoAssign(@RequestBody Map<String, Long> body) {

    Long examId = body.get("examId");
    Long collegeId = body.get("collegeId");

    Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));

    College college = collegeRepository.findById(collegeId)
            .orElseThrow(() -> new RuntimeException("College not found"));

    // Find available supervisor
    List<Supervisor> availableSupervisors = supervisorRepository.findByAvailability("Available");

    if (availableSupervisors.isEmpty()) {
        throw new RuntimeException("No available supervisors");
    }

    Supervisor supervisor = availableSupervisors.get(0);

    ExamAssignment assignment = new ExamAssignment();
    assignment.setSupervisor(supervisor);
    assignment.setExam(exam);
    assignment.setCollege(college);
    assignment.setStatus("Assigned");

    // Mark supervisor as Assigned
    supervisor.setAvailability("Assigned");
    supervisorRepository.save(supervisor);

    return assignmentRepository.save(assignment);
}

    @PostMapping
    public ExamAssignment assignSupervisor(@RequestBody Map<String, Long> body) {

        Long supervisorId = body.get("supervisorId");
        Long examId = body.get("examId");
        Long collegeId = body.get("collegeId");

        Supervisor supervisor = supervisorRepository.findById(supervisorId).orElse(null);
        Exam exam = examRepository.findById(examId).orElse(null);
        College college = collegeRepository.findById(collegeId).orElse(null);

        if (supervisor == null || exam == null || college == null) {
            throw new RuntimeException("Invalid IDs");
        }

        // ✅ Duplicate check BEFORE saving
        if (assignmentRepository.existsBySupervisorAndExamAndCollege(
                supervisor, exam, college)) {

            throw new RuntimeException("Assignment already exists!");
        }

        ExamAssignment assignment = new ExamAssignment();
        assignment.setSupervisor(supervisor);
        assignment.setExam(exam);
        assignment.setCollege(college);

        return assignmentRepository.save(assignment);
    }
}