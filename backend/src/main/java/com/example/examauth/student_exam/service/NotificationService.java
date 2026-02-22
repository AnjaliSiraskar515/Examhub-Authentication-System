package com.example.examauth.student_exam.service;

import com.example.examauth.student_exam.dto.NotificationResponseDTO;
import com.example.examauth.student_exam.model.Notification;
import com.example.examauth.student_exam.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponseDTO> getNotificationsForStudent(Long studentId) {
        return notificationRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void createNotification(Long studentId, String title, String message) {
        if (studentId == null) {
            log.error("Cannot create notification: studentId is NULL. Title: '{}', Message: '{}'", title, message);
            throw new IllegalArgumentException("studentId cannot be null when creating a notification");
        }

        log.info("Creating notification for studentId: {} with title: '{}'", studentId, title);
        Notification notification = new Notification();
        notification.setStudentId(studentId);
        notification.setTitle(title);
        notification.setMessage(message);
        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved successfully with ID: {} for studentId: {}", saved.getId(), studentId);
    }

    private NotificationResponseDTO mapToDTO(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getCreatedAt().toString());
    }
}
