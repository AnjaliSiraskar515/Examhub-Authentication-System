package com.example.examauth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.examauth.student_exam.repo.ExamRegistrationRepository;
import com.example.examauth.student_exam.model.ExamRegistration;
import java.util.List;

import com.example.examauth.student_exam.service.ExamRegistrationService;
import com.example.examauth.student_exam.dto.ExamRegistrationResponseDTO;

@RestController
public class DebugRegistrationController {

    @Autowired
    private ExamRegistrationService service;

    @GetMapping("/api/debug/registrations")
    public List<ExamRegistrationResponseDTO> getAll() {
        return service.getRegistrationsByStudentId(1L);
    }
}
