package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.NotificationResponseDTO;
import com.example.examauth.student_exam.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/notifications")
@RequiredArgsConstructor
public class StudentNotificationController {

    private final NotificationService notificationService;
    private final com.example.examauth.repo.UserRepository userRepository;

    private Long resolveStudentId(Authentication authentication) {
        Long studentId = 1L;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            com.example.examauth.model.User user = userRepository.findFirstByEmailAndRole(
                email, authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
            ).orElse(null);
            if (user != null) {
                studentId = user.getUserId();
            }
        }
        return studentId;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getAllNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getNotificationsForStudent(resolveStudentId(authentication)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(resolveStudentId(authentication));
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
