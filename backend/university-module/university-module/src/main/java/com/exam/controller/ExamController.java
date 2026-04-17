package com.exam.controller;

import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.exam.entity.*;
import com.exam.repository.*;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/exams")
public class ExamController {

   
    
    @Autowired
     private final ExamRepository examRepository;

     public ExamController(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }


    @Autowired
    private ExamStudentMappingRepository mappingRepository;

    @Autowired
    private ExamScheduleRepository scheduleRepository;

    @GetMapping("/create")
    public Exam createExam(@RequestBody Exam exam){
        if(exam.getExamDate().isBefore(LocalDate.now())){
            throw new RuntimeException("Exam date cannot be past date");
        }
        return examRepository.save(exam);
    }

    @PutMapping("/{id}")
public Exam updateExam(@PathVariable Long id, @RequestBody Exam updatedExam) {

    Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Exam not found"));

    exam.setExamDate(updatedExam.getExamDate());

    return examRepository.save(exam);
}

   

      @GetMapping("/test")
    public String test() {
    return "Backend working!";
}
    @GetMapping
public List<Exam> getAllExams() {
    return examRepository.findAll();
}

    @GetMapping("/all")
    public List<Exam> getAll(){
        return examRepository.findAll();
    }

    @PutMapping("/activate/{id}")
    public Exam activateExam(@PathVariable Long id){
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        exam.setActive(true);
        return examRepository.save(exam);
    }

    @PutMapping("/deactivate/{id}")
    public Exam deactivateExam(@PathVariable Long id){
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        exam.setActive(false);
        return examRepository.save(exam);
    }

    @GetMapping("/assign")
    public ExamStudentMapping assignStudent(@RequestBody ExamStudentMapping mapping){
        return mappingRepository.save(mapping);
    }

   @GetMapping("/dashboard")
public Map<String, Long> getDashboardStats() {

    long total = examRepository.count();

    long active = examRepository.findAll()
            .stream()
            .filter(exam -> exam.getExamDate().isAfter(LocalDate.now()))
            .count();

    Map<String, Long> stats = new HashMap<>();
    stats.put("totalExams", total);
    stats.put("activeExams", active);

    return stats;
}


    @GetMapping("/validate/{examId}")
    public boolean validateExam(@PathVariable Long examId){
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        return exam.isActive();
    }

    @GetMapping("/schedule")
    public ExamSchedule createSchedule(@RequestBody ExamSchedule schedule){
        return scheduleRepository.save(schedule);
    }

    @GetMapping("/schedules")
    public List<ExamSchedule> getSchedules(){
        return scheduleRepository.findAll();
    }

    @GetMapping("/status/{examId}")
    public String checkExamStatus(@PathVariable Long examId){

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        if(!exam.isActive()){
            return "Exam is not active";
        }

        List<ExamSchedule> schedules = scheduleRepository.findAll();

        for(ExamSchedule s : schedules){
            if(s.getExamId().equals(examId)){
                if(s.getExamDate().equals(LocalDate.now())){
                    return "Exam running today";
                }
            }
        }
        return "Exam not scheduled today";
    }

    @DeleteMapping("/{id}")
public void deleteExam(@PathVariable Long id) {
    examRepository.deleteById(id);
}

@GetMapping("/hello")
public String hello() {
    return "Controller is working!";
}

   
}
