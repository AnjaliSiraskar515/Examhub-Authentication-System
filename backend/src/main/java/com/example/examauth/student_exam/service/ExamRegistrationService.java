package com.example.examauth.student_exam.service;

import com.example.examauth.student_exam.dto.ExamRegistrationRequestDTO;
import com.example.examauth.student_exam.dto.ExamRegistrationResponseDTO;
import com.example.examauth.student_exam.exception.AlreadyRegisteredException;
import com.example.examauth.student_exam.exception.ExamNotFoundException;
import com.example.examauth.student_exam.model.ExamRegistration;
import com.example.examauth.student_exam.repo.ExamRegistrationRepository;
import com.example.examauth.service.AiVerificationClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamRegistrationService {

    private final ExamRegistrationRepository examRegistrationRepository;
    private final com.example.examauth.student_exam.service.NotificationService notificationService;
    private final AiVerificationClientService aiVerificationClientService;

    private final com.example.examauth.student_exam.university.repo.UniversityExamRepository universityExamRepository;
    private final com.example.examauth.student_exam.university.repo.ExamEligibleStudentRepository examEligibleStudentRepository;

    /**
     * SECURE REGISTRATION METHOD WITH PROFESSIONAL VALIDATION
     * Registers a student for an exam after comprehensive validation.
     */
    public ExamRegistrationResponseDTO registerStudent(ExamRegistrationRequestDTO request) {
        String examName = "Unknown Exam";

        // ========== VALIDATION 1: Declaration Acceptance ==========
        if (request.getDeclarationAccepted() != null && !request.getDeclarationAccepted()) {
            throw new IllegalArgumentException("Declaration must be accepted to proceed with registration");
        }

        // 1. Check if Exam exists
        com.example.examauth.student_exam.university.model.UniversityExam exam = universityExamRepository
                .findById(request.getExamId())
                .orElseThrow(() -> new ExamNotFoundException("Exam not found with ID: " + request.getExamId()));
        examName = exam.getExamName();

        // ========== VALIDATION 1.5: Max Students Capacity Check ==========
        if (exam.getControls() != null && exam.getControls().getMaxStudents() != null) {
            long currentRegistrations = examRegistrationRepository.countByExamIdAndRegistrationStatus(request.getExamId(), ExamRegistration.RegistrationStatus.APPROVED);
            if (currentRegistrations >= exam.getControls().getMaxStudents()) {
                throw new IllegalStateException("Registration is closed: Maximum student capacity (" + exam.getControls().getMaxStudents() + ") has been reached for this exam.");
            }
        }

        // ========== VALIDATION 2: Eligible Subjects Check ==========
        if (request.getSelectedSubjects() != null && !request.getSelectedSubjects().isEmpty()) {
            // Fetch eligible student record from ExamEligibleStudent table
            java.util.Optional<com.example.examauth.student_exam.university.model.ExamEligibleStudent> eligibleStudentOpt = examEligibleStudentRepository
                    .findByPrnNumberAndExamSession(request.getPrn(), request.getExamSession());

            if (eligibleStudentOpt.isPresent()) {
                com.example.examauth.student_exam.university.model.ExamEligibleStudent eligibleStudent = eligibleStudentOpt
                        .get();
                List<String> eligibleSubjects = eligibleStudent.getEligibleSubjects();

                // Check if all selected subjects are in the eligible list
                for (String selectedSubject : request.getSelectedSubjects()) {
                    if (!eligibleSubjects.contains(selectedSubject)) {
                        throw new IllegalArgumentException(
                                "Invalid subject selection: '" + selectedSubject +
                                        "' is not in your eligible subjects list for this exam session");
                    }
                }
            }
            // If no eligible record found, allow registration (backward compatibility)
            // In production, you might want to enforce this check
        }

        // ========== VALIDATION 3: Duplicate Registration Check ==========
        // Primary check by studentId (set from authenticated user in controller)
        if (request.getStudentId() != null &&
                examRegistrationRepository.existsByStudentIdAndExamId(request.getStudentId(), request.getExamId())) {
            throw new AlreadyRegisteredException("Already registered: You have already registered for this exam.");
        }
        // Secondary check by PRN (only when studentId unavailable)
        if (request.getStudentId() == null &&
                request.getPrn() != null && !request.getPrn().startsWith("TEMP-") &&
                examRegistrationRepository.existsByPrnAndExamId(request.getPrn(), request.getExamId())) {
            throw new AlreadyRegisteredException("Already registered: PRN " + request.getPrn()
                    + " is already registered for this exam.");
        }

        // 4. Save Registration
        ExamRegistration registration = new ExamRegistration();
        registration.setStudentId(request.getStudentId());
        registration.setExamId(request.getExamId());
        registration.setPrn(request.getPrn());
        registration.setFullName(request.getFullName());
        registration.setCourse(request.getCourse());
        registration.setYear(request.getYear());
        registration.setInstitutionName(request.getInstitutionName());
        registration.setExamSession(request.getExamSession());
        registration.setAppliedDate(LocalDate.now());

        // Set new professional fields if provided
        if (request.getSelectedSubjects() != null) {
            registration.setSelectedSubjects(request.getSelectedSubjects());
        }
        if (request.getExamType() != null) {
            registration.setExamType(request.getExamType());
        }
        if (request.getDeclarationAccepted() != null) {
            registration.setDeclarationAccepted(request.getDeclarationAccepted());
        }
        if (request.getPaymentStatus() != null) {
            registration.setPaymentStatus(ExamRegistration.PaymentStatus.valueOf(request.getPaymentStatus()));
        }
        if (request.getTotalFee() != null) {
            registration.setTotalFee(request.getTotalFee());
        }

        // Use PENDING_APPROVAL for competitive registration workflow
        registration.setRegistrationStatus(ExamRegistration.RegistrationStatus.PENDING_APPROVAL);

        ExamRegistration savedRegistration = examRegistrationRepository.save(registration);

        // 5. Create Notification for Student (with null check)
        if (request.getStudentId() != null) {
            String message = "You have successfully registered for " + examName
                    + " exam. You will be notified after approval.";
            log.info("Creating notification for studentId: {} for exam: {}", request.getStudentId(), examName);
            notificationService.createNotification(request.getStudentId(), "Exam Registration Successful", message);
            log.info("Notification created successfully for studentId: {}", request.getStudentId());
        } else {
            log.warn("Notification NOT created: studentId is NULL for registration ID: {}", savedRegistration.getId());
        }

        return mapToResponseDTO(savedRegistration);
    }

    /**
     * OLD METHOD (kept for backward compatibility - Deprecated)
     */
    public ExamRegistrationResponseDTO registerForExam(Long studentId, Long examId) {
        return registerStudent(
                new ExamRegistrationRequestDTO(studentId, examId, "UNKNOWN", "Unknown", null, null, null, null, null));
    }

    /**
     * AI based registration (Optional / Alternative flow)
     */
    public ExamRegistrationResponseDTO registerForExam(
            Long studentId,
            Long examId,
            MultipartFile document) {

        Map<String, Object> aiResult = aiVerificationClientService.verifyDocument(document);

        double confidence = ((Number) aiResult.getOrDefault("confidence", 0.0)).doubleValue();

        ExamRegistration registration = new ExamRegistration();
        registration.setStudentId(studentId);
        registration.setExamId(examId);
        // Default values for new fields if not provided
        registration.setPrn("TEMP-" + studentId);
        registration.setAppliedDate(LocalDate.now());

        if (confidence >= 0.9) {
            registration.setRegistrationStatus(
                    ExamRegistration.RegistrationStatus.APPROVED);
        } else {
            registration.setRegistrationStatus(
                    ExamRegistration.RegistrationStatus.PENDING);
        }

        ExamRegistration savedRegistration = examRegistrationRepository.save(registration);

        return mapToResponseDTO(savedRegistration);
    }

    private ExamRegistrationResponseDTO mapToResponseDTO(ExamRegistration registration) {
        ExamRegistrationResponseDTO dto = new ExamRegistrationResponseDTO();
        dto.setId(registration.getId());
        dto.setStudentId(registration.getStudentId());
        dto.setExamId(registration.getExamId());
        dto.setPrn(registration.getPrn());
        dto.setFullName(registration.getFullName());
        dto.setCourse(registration.getCourse());
        dto.setExamSession(registration.getExamSession());
        dto.setSelectedSubjects(registration.getSelectedSubjects());
        dto.setTotalFee(registration.getTotalFee());
        dto.setPaymentStatus(
                registration.getRegistrationStatus() == ExamRegistration.RegistrationStatus.APPROVED 
                ? "PAID" 
                : (registration.getPaymentStatus() != null ? registration.getPaymentStatus().name() : "PENDING")
        );
        dto.setExamType(registration.getExamType());
        dto.setRegistrationStatus(
                registration.getRegistrationStatus() != null ? registration.getRegistrationStatus().name() : "APPLIED");
        dto.setAppliedDate(registration.getAppliedDate() != null ? registration.getAppliedDate().toString() : null);
        dto.setHallTicketReleased(registration.getHallTicketReleased());
        return dto;
    }

    public List<ExamRegistrationResponseDTO> getRegistrationsByStudentId(Long studentId) {
        List<ExamRegistration> registrations = examRegistrationRepository.findByStudentId(studentId);

        return registrations.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get student statistics (total, pending, approved, upcoming exams)
     */
    public com.example.examauth.student_exam.dto.StudentStatsDTO getStudentStats(Long studentId) {
        List<ExamRegistration> registrations = examRegistrationRepository.findByStudentId(studentId);

        int totalRegistered = registrations.size();
        int pendingVerifications = (int) registrations.stream()
                .filter(r -> r.getRegistrationStatus() == ExamRegistration.RegistrationStatus.PENDING)
                .count();
        int approvedExams = (int) registrations.stream()
                .filter(r -> r.getRegistrationStatus() == ExamRegistration.RegistrationStatus.APPROVED)
                .count();

        // Count upcoming exams (approved + exam date in future)
        LocalDate today = LocalDate.now();
        int upcomingExams = 0;

        for (ExamRegistration reg : registrations) {
            if (reg.getRegistrationStatus() == ExamRegistration.RegistrationStatus.APPROVED) {
                try {
                    com.example.examauth.student_exam.university.model.UniversityExam exam = universityExamRepository
                            .findById(reg.getExamId()).orElse(null);
                    if (exam != null) {
                        String status = exam.getStatus() != null ? exam.getStatus() : "";
                        if (!"COMPLETED".equalsIgnoreCase(status)) {
                            // If it's not completed, it's either upcoming or currently ongoing
                            upcomingExams++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error fetching exam for stats: {}", e.getMessage());
                }
            }
        }

        return new com.example.examauth.student_exam.dto.StudentStatsDTO(
                totalRegistered,
                pendingVerifications,
                approvedExams,
                upcomingExams);
    }
}
