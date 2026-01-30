package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.service.ExamRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/registrations")
@RequiredArgsConstructor
public class StudentRegistrationController {

    private final ExamRegistrationService examRegistrationService;

    // TODO: Add endpoints for student exam registration operations
}
