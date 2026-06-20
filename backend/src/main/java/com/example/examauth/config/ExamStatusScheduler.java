package com.example.examauth.config;

import com.example.examauth.student_exam.model.ExamHall;
import com.example.examauth.student_exam.model.ExamRegistration;
import com.example.examauth.student_exam.model.ExamSeatAllocation;
import com.example.examauth.student_exam.repo.ExamHallRepository;
import com.example.examauth.student_exam.repo.ExamRegistrationRepository;
import com.example.examauth.student_exam.repo.ExamSeatAllocationRepository;
import com.example.examauth.student_exam.service.NotificationService;
import com.example.examauth.student_exam.university.model.UniversityExam;
import com.example.examauth.student_exam.university.model.UniversityExamSubject;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.model.User;
import com.example.examauth.student_exam.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExamStatusScheduler {

    private final UniversityExamRepository universityExamRepository;
    private final ExamRegistrationRepository examRegistrationRepository;
    private final ExamHallRepository examHallRepository;
    private final ExamSeatAllocationRepository examSeatAllocationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    // Track notifications by Slot: format "examId_subjectId"
    private final java.util.Set<String> studentNotifiedSlots = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> supervisorNotifiedSlots = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Transactional
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void updateExamStatuses() {
        try {
            LocalDateTime now = LocalDateTime.now();

            List<UniversityExam> exams = universityExamRepository.findAll().stream()
                    .filter(e -> !"COMPLETED".equalsIgnoreCase(e.getStatus()))
                    .toList();

            int liveCount = 0, completedCount = 0, openCount = 0;

            for (UniversityExam exam : exams) {
                if (exam.getSubjects() == null || exam.getSubjects().isEmpty()) continue;

                boolean isLive = false;
                boolean allCompleted = true;

                for (UniversityExamSubject sub : exam.getSubjects()) {
                    if (sub.getExamDate() == null || sub.getStartTime() == null) {
                        allCompleted = false;
                        continue;
                    }

                    LocalTime eTime = sub.getEndTime() != null ? sub.getEndTime() : sub.getStartTime().plusHours(3);
                    LocalDateTime sDT = LocalDateTime.of(sub.getExamDate(), sub.getStartTime());
                    LocalDateTime eDT = LocalDateTime.of(sub.getExamDate(), eTime);
                    if (eTime.isBefore(sub.getStartTime())) {
                        eDT = eDT.plusDays(1);
                    }

                    if (!now.isBefore(sDT) && now.isBefore(eDT)) {
                        isLive = true;
                        allCompleted = false;
                    } else if (now.isBefore(sDT)) {
                        allCompleted = false;
                    }
                }

                if (isLive) {
                    if (!"LIVE".equalsIgnoreCase(exam.getStatus())) {
                        exam.setStatus("LIVE");
                        universityExamRepository.save(exam);
                        liveCount++;
                    }
                } else if (allCompleted) {
                    if (!"COMPLETED".equalsIgnoreCase(exam.getStatus())) {
                        exam.setStatus("COMPLETED");
                        universityExamRepository.save(exam);
                        completedCount++;
                    }
                } else {
                    // Between subjects, ensure it's not stuck on LIVE
                    if ("LIVE".equalsIgnoreCase(exam.getStatus())) {
                        exam.setStatus("OPEN");
                        universityExamRepository.save(exam);
                        openCount++;
                    }
                }
            }

            if (liveCount > 0 || completedCount > 0 || openCount > 0) {
                log.info("Scheduler: {} set to LIVE, {} set to COMPLETED, {} reverted to OPEN", liveCount, completedCount, openCount);
            }
        } catch (Exception e) {
            log.error("updateExamStatuses failed: {}", e.getMessage());
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void notifyStudentsOfHall() {
        try {
            LocalDateTime now = LocalDateTime.now();

            List<UniversityExam> exams = universityExamRepository.findAll().stream()
                    .filter(e -> !"COMPLETED".equalsIgnoreCase(e.getStatus()))
                    .toList();

            for (UniversityExam exam : exams) {
                if (exam.getSubjects() == null) continue;

                // Get all approved registrations for this exam once
                List<ExamRegistration> approved = examRegistrationRepository.findAll().stream()
                        .filter(r -> r.getExamId().equals(exam.getId()))
                        .filter(r -> r.getRegistrationStatus() == ExamRegistration.RegistrationStatus.APPROVED)
                        .toList();

                if (approved.isEmpty()) continue;

                for (UniversityExamSubject sub : exam.getSubjects()) {
                    if (sub.getExamDate() == null || sub.getStartTime() == null) continue;

                    LocalDateTime sDT = LocalDateTime.of(sub.getExamDate(), sub.getStartTime());
                    LocalDateTime twoHoursBefore = sDT.minusHours(2);

                    // Only fire in the 2-hour window before this subject starts
                    if (now.isBefore(twoHoursBefore) || !now.isBefore(sDT)) continue;

                    String sessionName = exam.getSessionName() != null ? exam.getSessionName() : "Upcoming Exam";

                    for (ExamRegistration reg : approved) {
                        // Only notify if student selected this subject (or if no subjects selected = all-subject exam)
                        List<String> selectedSubjects = reg.getSelectedSubjects();
                        boolean isRelevant = selectedSubjects == null || selectedSubjects.isEmpty()
                                || selectedSubjects.stream().anyMatch(s -> s.equalsIgnoreCase(sub.getSubjectName()));
                        if (!isRelevant) continue;

                        String title = "Exam Starting Soon — " + sub.getSubjectName() + " (" + sub.getExamDate() + ")";

                        // Query database to prevent duplicates across restarts
                        if (notificationRepository.existsByStudentIdAndTitle(reg.getStudentId(), title)) continue;

                        // Fetch seat allocation for hall and seat number
                        ExamSeatAllocation seat = examSeatAllocationRepository
                                .findByRegistrationIdAndSubjectId(reg.getId(), sub.getId()).orElse(null);
                        
                        if (seat == null) {
                            seat = examSeatAllocationRepository.findFirstByRegistrationId(reg.getId()).orElse(null);
                        }

                        String hallInfo = (seat != null && seat.getHallName() != null && !seat.getHallName().isBlank())
                                ? seat.getHallName() : "Not yet assigned";
                        String seatInfo = (seat != null && seat.getSeatNumber() != null && !seat.getSeatNumber().isBlank())
                                ? " | Seat No: " + seat.getSeatNumber() : "";
                        // Use same roll number pattern as ProfileController (dept prefix + userId)
                        User student = userRepository.findById(reg.getStudentId()).orElse(null);
                        String rollInfo = "";
                        if (student != null) {
                            String deptPrefix = (student.getDepartment() != null && student.getDepartment().length() >= 2)
                                    ? student.getDepartment().substring(0, 2).toUpperCase() : "GN";
                            String studentRoll = String.format("%s-%04d", deptPrefix, student.getUserId());
                            rollInfo = " | Roll No: " + studentRoll;
                        }

                        String message = "Your " + sub.getSubjectName() + " exam for " + sessionName
                                + " starts at " + sub.getStartTime() + " on " + sub.getExamDate() + "."
                                + " Hall: " + hallInfo + seatInfo + rollInfo
                                + ". Please report 15 minutes before with your Hall Ticket.";

                        try {
                            notificationService.createNotification(reg.getStudentId(), title, message);
                            log.info("Hall notification sent to student {} for subject {}",
                                    reg.getStudentId(), sub.getSubjectName());
                        } catch (Exception ex) {
                            log.warn("Could not notify student {}: {}", reg.getStudentId(), ex.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("notifyStudentsOfHall failed: {}", e.getMessage());
        }
    }


    @Transactional
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void notifySupervisorsOfDuty() {
        try {
            LocalDateTime now = LocalDateTime.now();

            List<UniversityExam> exams = universityExamRepository.findAll().stream()
                    .filter(e -> !"COMPLETED".equalsIgnoreCase(e.getStatus()))
                    .toList();

            for (UniversityExam exam : exams) {
                if (exam.getSubjects() == null) continue;

                for (UniversityExamSubject sub : exam.getSubjects()) {
                    if (sub.getExamDate() == null || sub.getStartTime() == null) continue;

                    String slotKey = exam.getId() + "_" + sub.getId();
                    if (supervisorNotifiedSlots.contains(slotKey)) continue;

                    LocalDateTime sDT = LocalDateTime.of(sub.getExamDate(), sub.getStartTime());
                    LocalDateTime fourHoursBefore = sDT.minusHours(4);

                    if (!now.isBefore(fourHoursBefore) && now.isBefore(sDT)) {
                        List<ExamHall> halls = examHallRepository.findAllByExamId(exam.getId());
                        String sessionName = exam.getSessionName() != null ? exam.getSessionName() : "Upcoming Exam";

                        for (ExamHall hall : halls) {
                            if (hall.getExamSupervisorId() != null) {
                                String title = "Duty Reminder — " + sub.getSubjectName();
                                String message = "Reminder: You are assigned to invigilate "
                                        + hall.getHallName() + " for the " + sub.getSubjectName()
                                        + " exam starting at " + sub.getStartTime()
                                        + ". Please report to the exam center by " + sub.getStartTime().minusHours(3) + ".";
                                try {
                                    notificationService.createNotification(hall.getExamSupervisorId(), title, message);
                                } catch (Exception ex) {
                                    log.warn("Could not notify supervisor {}: {}", hall.getExamSupervisorId(), ex.getMessage());
                                }
                            }
                        }

                        if (exam.getSupervisorId() != null) {
                            String title = "Head Supervisor Reminder — " + sub.getSubjectName();
                            String message = "Reminder: The " + sub.getSubjectName() + " slot for " + sessionName
                                    + " starts at " + sub.getStartTime() + ". Ensure all halls are prepared.";
                            try {
                                notificationService.createNotification(exam.getSupervisorId(), title, message);
                            } catch (Exception ex) {
                                log.warn("Could not notify head supervisor {}: {}", exam.getSupervisorId(), ex.getMessage());
                            }
                        }

                        supervisorNotifiedSlots.add(slotKey);
                    }
                }
            }
        } catch (Exception e) {
            log.error("notifySupervisorsOfDuty failed: {}", e.getMessage());
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void expirePendingRegistrations() {
        try {
            LocalDateTime now = LocalDateTime.now();

            List<ExamRegistration> pendingRegistrations = examRegistrationRepository
                    .findByRegistrationStatus(ExamRegistration.RegistrationStatus.PENDING_APPROVAL);

            for (ExamRegistration reg : pendingRegistrations) {
                UniversityExam exam = universityExamRepository.findById(reg.getExamId()).orElse(null);
                if (exam == null) continue;

                boolean shouldExpire = false;

                // Check registration window if exists
                if (exam.getRegistrationWindow() != null && exam.getRegistrationWindow().getEndDate() != null) {
                    if (now.toLocalDate().isAfter(exam.getRegistrationWindow().getEndDate())) {
                        shouldExpire = true;
                    }
                } else if (exam.getExamDate() != null) {
                    // Fallback to exam date
                    if (now.toLocalDate().isAfter(exam.getExamDate())) {
                        shouldExpire = true;
                    }
                }

                if (shouldExpire) {
                    reg.setRegistrationStatus(ExamRegistration.RegistrationStatus.AUTO_EXPIRED);
                    examRegistrationRepository.save(reg);

                    notificationService.createNotification(reg.getStudentId(), "Exam Registration Expired",
                            "Your registration for " + exam.getExamName() + " has expired as the deadline has passed without approval.");
                }
            }
        } catch (Exception e) {
            log.error("expirePendingRegistrations failed: {}", e.getMessage());
        }
    }
}
