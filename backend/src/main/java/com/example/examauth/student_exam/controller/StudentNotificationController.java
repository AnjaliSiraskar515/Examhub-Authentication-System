package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.NotificationResponseDTO;
import com.example.examauth.student_exam.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/notifications")
@RequiredArgsConstructor
public class StudentNotificationController {

    private final NotificationService notificationService;

    private final com.example.examauth.repo.UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getAllNotifications(
            org.springframework.security.core.Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        com.example.examauth.model.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(notificationService.getNotificationsForStudent(user.getUserId()));
    }
}
