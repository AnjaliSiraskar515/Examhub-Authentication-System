package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/notifications")
@RequiredArgsConstructor
public class StudentNotificationController {

    private final NotificationService notificationService;

    // TODO: Add endpoints for student notification operations
}
