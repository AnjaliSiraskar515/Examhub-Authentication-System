package com.exam.controller;

import com.exam.repository.ExamAssignmentRepository;
import com.exam.entity.ExamAssignment;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.exam.entity.Supervisor;
import com.exam.repository.SupervisorRepository;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
@RestController
@RequestMapping("/api/supervisors")
@CrossOrigin(origins = "*")
public class SupervisorController {

    @Autowired
private SupervisorRepository supervisorRepository;

    @Autowired
    private ExamAssignmentRepository assignmentRepository;

    @Autowired
    private SupervisorRepository repository;

    @GetMapping
    public List<Supervisor> getAllSupervisors() {
    return supervisorRepository.findAll();
}

    @PostMapping
    public Supervisor add(@RequestBody Supervisor supervisor) {
        return repository.save(supervisor);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @GetMapping("/{id}/profile")
public Map<String, Object> getSupervisorProfile(@PathVariable Long id) {

    Supervisor supervisor = supervisorRepository.findById(id).orElse(null);

    if (supervisor == null) {
        return Map.of("error", "Supervisor not found");
    }

    List<ExamAssignment> assignments =
            assignmentRepository.findBySupervisor(supervisor);

    List<Map<String, String>> examList = assignments.stream().map(a -> {
        Map<String, String> map = new HashMap<>();
        map.put("examName", a.getExam().getExamName());
        map.put("collegeName", a.getCollege().getCollegeName());
        return map;
    }).toList();

    Map<String, Object> response = new HashMap<>();
    response.put("id", supervisor.getId());
    response.put("name", supervisor.getName());
    response.put("email", supervisor.getEmail());
    response.put("assignedExams", examList);

    return response;
}
@GetMapping("/test")
public String test() {
    return "Working";
}

@GetMapping("/hello")
public String hello() {
    return "Supervisor working!";
}
}
