package com.example.examauth.exam_registration;

import com.example.examauth.model.Exam;
import com.example.examauth.repo.ExamRepository;
import com.example.examauth.student_exam.model.Notification;
import com.example.examauth.student_exam.repo.NotificationRepository;
import com.example.examauth.student_profile.model.StudentProfile;
import com.example.examauth.student_profile.repo.StudentProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service("examRegistrationModuleService")
public class ExamRegistrationService {

    private final ExamRegistrationRepository examRegistrationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ExamRepository examRepository;
    private final NotificationRepository notificationRepository;

    public ExamRegistrationService(
            ExamRegistrationRepository examRegistrationRepository,
            StudentProfileRepository studentProfileRepository,
            ExamRepository examRepository,
            NotificationRepository notificationRepository
    ) {
        this.examRegistrationRepository = examRegistrationRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.examRepository = examRepository;
        this.notificationRepository = notificationRepository;
    }

    public void registerForExam(Long studentId, Long examId) {
        // 1) Check StudentProfile exists
        StudentProfile profile = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        // 2) Check profile.isVerified() == true
        if (!profile.isVerified()) {
            throw new RuntimeException("Student profile is not verified");
        }

        // 3) Check profile.isProfileLocked() == true
        if (!profile.isProfileLocked()) {
            throw new RuntimeException("Student profile is not locked");
        }

        // 4) Check exam exists
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        // 5) Check exam status is ACTIVE
        if (exam.getStatus() == null || !exam.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new RuntimeException("Exam is not active");
        }

        // 6) Check student not already registered for same exam
        if (examRegistrationRepository.existsByStudentIdAndExamId(studentId, examId)) {
            throw new RuntimeException("Student already registered for this exam");
        }

        // 7) Create ExamRegistration record
        ExamRegistration registration = new ExamRegistration();
        registration.setStudentId(studentId);
        registration.setExamId(examId);
        registration.setRegistrationDate(LocalDateTime.now());
        registration.setStatus("REGISTERED");
        registration.setHallTicketGenerated(false);

        examRegistrationRepository.save(registration);

        // Create Notification
        Notification notification = new Notification();
        notification.setTitle("Exam Registration");
        notification.setMessage("You have successfully registered for exam: " + exam.getExamName());
        notification.setStudentId(studentId);
        notificationRepository.save(notification);
    }

    public List<ExamRegistration> getRegistrationsForStudent(Long studentId) {
        return examRegistrationRepository.findByStudentId(studentId);
    }
}
