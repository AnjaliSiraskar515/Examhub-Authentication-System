package com.example.examauth.student_exam.university;

import com.example.examauth.student_exam.dto.ExamRegistrationResponseDTO;
import com.example.examauth.student_exam.model.ExamRegistration;
import com.example.examauth.student_exam.repo.ExamRegistrationRepository;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import com.example.examauth.student_exam.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityExamRepository examRepository;
    private final ExamRegistrationRepository registrationRepository;
    private final NotificationService notificationService;

    public DashboardStatsDTO getDashboardStats() {
        long activeExams = examRepository.count();
        long totalRegistrations = registrationRepository.count();
        long approvedRegistrations = registrationRepository
                .countByRegistrationStatus(ExamRegistration.RegistrationStatus.APPROVED);
        long pendingApprovals = registrationRepository
                .countByRegistrationStatus(ExamRegistration.RegistrationStatus.PENDING);

        return DashboardStatsDTO.builder()
                .activeExams(activeExams)
                .totalRegistrations(totalRegistrations)
                .approvedRegistrations(approvedRegistrations)
                .pendingApprovals(pendingApprovals)
                .totalStudents(0) // Placeholder
                .build();
    }

    public List<ExamRegistration> getAllRegistrations() {
        return registrationRepository.findAll();
    }

    @Transactional
    public void approveRegistration(Long id) {
        ExamRegistration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
                
        com.example.examauth.student_exam.university.model.UniversityExam exam = examRepository.findById(registration.getExamId())
                .orElseThrow(() -> new RuntimeException("Exam not found"));
                
        // Lock/Check Capacity
        if (exam.getControls() != null && exam.getControls().getMaxStudents() != null) {
            long approvedCount = registrationRepository.countByExamIdAndRegistrationStatus(exam.getId(), ExamRegistration.RegistrationStatus.APPROVED);
            if (approvedCount >= exam.getControls().getMaxStudents()) {
                throw new IllegalStateException("Cannot approve: Capacity already reached for this exam.");
            }
            
            // Re-check: Will this approval reach the max capacity?
            if (approvedCount + 1 == exam.getControls().getMaxStudents()) {
                // Auto-reject others
                List<ExamRegistration> pendingRegistrations = registrationRepository.findByExamIdAndRegistrationStatus(exam.getId(), ExamRegistration.RegistrationStatus.PENDING_APPROVAL);
                for (ExamRegistration pendingReg : pendingRegistrations) {
                    if (!pendingReg.getId().equals(registration.getId())) {
                        pendingReg.setRegistrationStatus(ExamRegistration.RegistrationStatus.REJECTED);
                        pendingReg.setRejectionReason("The available seats for this session have been filled.");
                        registrationRepository.save(pendingReg);
                        notificationService.createNotification(pendingReg.getStudentId(), "Exam Registration Rejected",
                                "Your registration for " + exam.getExamName() + " was not approved because the available seats for this session have been filled. You may apply for future examination sessions if available.");
                    }
                }
            }
        }
        
        // Approve current student
        registration.setRegistrationStatus(ExamRegistration.RegistrationStatus.APPROVED);
        registrationRepository.save(registration);
    }

    public void rejectRegistration(Long id) {
        updateRegistrationStatus(id, ExamRegistration.RegistrationStatus.REJECTED);
    }

    private void updateRegistrationStatus(Long id, ExamRegistration.RegistrationStatus status) {
        ExamRegistration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
        registration.setRegistrationStatus(status);
        registrationRepository.save(registration);
    }
}
