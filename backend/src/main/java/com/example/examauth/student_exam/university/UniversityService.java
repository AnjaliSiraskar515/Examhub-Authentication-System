package com.example.examauth.student_exam.university;

import com.example.examauth.student_exam.dto.ExamRegistrationResponseDTO;
import com.example.examauth.student_exam.model.ExamRegistration;
import com.example.examauth.student_exam.repo.ExamRegistrationRepository;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityExamRepository examRepository;
    private final ExamRegistrationRepository registrationRepository;

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

    public void approveRegistration(Long id) {
        updateRegistrationStatus(id, ExamRegistration.RegistrationStatus.APPROVED);
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
