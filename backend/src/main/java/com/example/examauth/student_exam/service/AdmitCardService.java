package com.example.examauth.student_exam.service;

import com.example.examauth.model.User;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.service.QrService;
import com.example.examauth.student_exam.dto.AdmitCardDTO;
import com.example.examauth.student_exam.model.ExamRegistration;
import com.example.examauth.student_exam.model.ExamSeatAllocation;
import com.example.examauth.student_exam.repo.ExamRegistrationRepository;
import com.example.examauth.student_exam.repo.ExamSeatAllocationRepository;
import com.example.examauth.student_exam.university.model.UniversityExam;
import com.example.examauth.student_exam.university.model.UniversityExamSubject;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import com.example.examauth.student_profile.model.StudentProfile;
import com.example.examauth.student_profile.repo.StudentProfileRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdmitCardService {

    private final ExamRegistrationRepository examRegistrationRepository;
    private final UniversityExamRepository universityExamRepository;
    private final UserRepository userRepository;
    private final QrService qrService;
    private final StudentProfileRepository studentProfileRepository;
    private final ExamSeatAllocationRepository examSeatAllocationRepository;
    private final com.example.examauth.repo.CollegeRepository collegeRepository;

    public AdmitCardDTO getAdmitCard(Long registrationId) {
        System.out.println("Fetching admit card for regId: " + registrationId);

        ExamRegistration registration = examRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (registration.getRegistrationStatus() != ExamRegistration.RegistrationStatus.APPROVED || !Boolean.TRUE.equals(registration.getHallTicketReleased())) {
             throw new RuntimeException("Hall Ticket not available");
        }

        User student = userRepository.findById(registration.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        UniversityExam exam = universityExamRepository.findById(registration.getExamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

        // ── Seat Allocation ────────────────────────────────────────────────────
        // Use real allocation if generated; fallback to legacy format if not yet run.
        ExamSeatAllocation seatAlloc = examSeatAllocationRepository
                .findFirstByRegistrationId(registrationId).orElse(null);

        final String seatNumber;
        final String rollNumber;
        final String hallName;

        if (seatAlloc != null) {
            // null-safe: each field falls back if the denormalized column is blank
            seatNumber = firstNonBlank(seatAlloc.getSeatNumber(), buildSeatNumber(registration));
            rollNumber = seatAlloc.getRollNumber(); // may be null — handled below
            hallName   = firstNonBlank(seatAlloc.getHallName(), "Not Assigned");
        } else {
            // Allocation not yet generated — keep existing admit card working
            seatNumber = buildSeatNumber(registration);
            rollNumber = null;
            hallName   = "Not Assigned";
        }

        // Center code: numeric part only (e.g. "SRCOE-001" → "001", null → "")
        String rawCode = seatAlloc != null
                ? firstNonBlank(extractCenterCode(exam, student), "")
                : extractCenterCode(exam, student);
        String centerDisplayName = seatAlloc != null
                ? firstNonBlank(seatAlloc.getCollegeName(), extractCenterName(exam, student))
                : extractCenterName(exam, student);

        // ── QR Code — SECURE OPAQUE TOKEN ONLY ────────────────────────────────
        // QR stores ONLY: EXAMHUB-{regId}-{hash}
        // Hall name and seat are NEVER embedded — resolved server-side by supervisors only.
        String tokenData = "SECURE_EXAM_" + registration.getId() + "_" + registration.getPrn();
        String secureHash = generateSecureHash(tokenData);
        // Opaque token: students/public cannot derive any information from it
        String qrPayload = "EXAMHUB-" + registration.getId() + "-" + secureHash.substring(0, 16).toUpperCase();

        String qrImage = generateQrPngBase64(qrPayload);
        registration.setQrCode(qrImage);
        examRegistrationRepository.save(registration);

        // ── Build DTO ──────────────────────────────────────────────────────────
        AdmitCardDTO dto = new AdmitCardDTO();
        dto.setStudentName(firstNonBlank(student.getName(), "Student"));
        dto.setSeatNumber(seatNumber);
        dto.setPrn(firstNonBlank(registration.getPrn(), "N/A"));
        dto.setCourse(firstNonBlank(registration.getCourse(), student.getMajor(), exam.getCourse(), "N/A"));
        dto.setSemester(firstNonBlank(student.getSemester(), exam.getSemester(), "N/A"));
        dto.setExamName(firstNonBlank(exam.getSessionName(), exam.getExamName(), registration.getExamSession(), "N/A"));
        dto.setCenterCode(rawCode);
        dto.setCenterName(centerDisplayName);
        dto.setSubjects(buildSubjects(exam, registration));
        dto.setQrCode(registration.getQrCode());

        // Fetch Head Supervisor Signature
        Long targetCollegeId = (seatAlloc != null && seatAlloc.getCollegeId() != null) 
                ? seatAlloc.getCollegeId() 
                : (student.getCollege() != null ? student.getCollege().getId() : null);
        if (targetCollegeId != null) {
            User headSupervisor = userRepository.findByRole("SUPERVISOR").stream()
                    .filter(u -> "HEAD".equalsIgnoreCase(u.getSupervisorType()))
                    .filter(u -> u.getCollege() != null && u.getCollege().getId().equals(targetCollegeId))
                    .findFirst()
                    .orElse(null);
            
            if (headSupervisor != null && headSupervisor.getSignaturePath() != null) {
                // Return URL relative to backend
                String sigPath = headSupervisor.getSignaturePath();
                dto.setHeadSupervisorSignatureUrl(sigPath.startsWith("/") ? sigPath : "/" + sigPath);
            }
        }

        // Fetch University Identity
        User universityAdmin = userRepository.findByRole("UNIVERSITY_ADMIN").stream()
                .filter(u -> u.getUniversityLogoPath() != null && !u.getUniversityLogoPath().isEmpty())
                .findFirst()
                .orElseGet(() -> userRepository.findByRole("UNIVERSITY_ADMIN").stream().findFirst().orElse(null));
        if (universityAdmin != null) {
            if (universityAdmin.getUniversityLogoPath() != null) {
                String logoPath = universityAdmin.getUniversityLogoPath();
                dto.setUniversityLogoUrl(logoPath.startsWith("/") ? logoPath : "/uploads/logo/" + logoPath);
            }
            if (universityAdmin.getUniversityName() != null) {
                dto.setUniversityName(universityAdmin.getUniversityName());
            }
        }
        // Generate roll number the same way ProfileController does, so it matches the exam form display
        String deptPrefix = (student.getDepartment() != null && student.getDepartment().length() >= 2)
                ? student.getDepartment().substring(0, 2).toUpperCase() : "GN";
        String generatedRoll = String.format("%s-%04d", deptPrefix, student.getUserId() != null ? student.getUserId() : 0);
        // Use student's actual enrollmentNo if set, otherwise use the generated pattern
        String studentActualRoll = firstNonBlank(
                (student.getEnrollmentNo() != null && !student.getEnrollmentNo().equals("EN001") && !student.getEnrollmentNo().startsWith("EN0") 
                    ? student.getEnrollmentNo() : null),
                generatedRoll,
                student.getPrn()
        );
        if (studentActualRoll != null && !studentActualRoll.isBlank()) {
            dto.setRollNumber(studentActualRoll);
        }
        // Set hall name only when seat allocation is generated
        if (rollNumber != null && !rollNumber.isBlank()) {
            dto.setHallName(hallName);
        }
        return dto;
    }

    private List<AdmitCardDTO.SubjectScheduleDTO> buildSubjects(UniversityExam exam, ExamRegistration registration) {
        List<AdmitCardDTO.SubjectScheduleDTO> list = new ArrayList<>();
        String globalDate = exam.getSchedule() != null && exam.getSchedule().getExamDate() != null
                ? exam.getSchedule().getExamDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "TBD";
        String globalTime = exam.getSchedule() != null && exam.getSchedule().getStartTime() != null
                ? exam.getSchedule().getStartTime() + " - "
                        + (exam.getSchedule().getEndTime() != null ? exam.getSchedule().getEndTime() : "TBD")
                : firstNonBlank(exam.getReportingTime(), "TBD");

        List<String> selected = registration.getSelectedSubjects() != null ? registration.getSelectedSubjects()
                : List.of();
        List<UniversityExamSubject> examSubjects = exam.getSubjects() != null ? exam.getSubjects() : List.of();

        if (!selected.isEmpty()) {
            for (String subjectName : selected) {
                UniversityExamSubject matchingSubject = examSubjects.stream()
                        .filter(s -> s.getSubjectName() != null && s.getSubjectName().equalsIgnoreCase(subjectName))
                        .findFirst()
                        .orElse(null);

                String subjectDate = matchingSubject != null && matchingSubject.getExamDate() != null
                        ? matchingSubject.getExamDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : globalDate;
                String subjectTime = matchingSubject != null && matchingSubject.getStartTime() != null
                        ? matchingSubject.getStartTime() + " - " + (matchingSubject.getEndTime() != null ? matchingSubject.getEndTime() : "TBD")
                        : globalTime;

                list.add(new AdmitCardDTO.SubjectScheduleDTO(subjectName, subjectDate, subjectTime));
            }
            return list;
        }

        for (UniversityExamSubject subject : examSubjects) {
            String subjectDate = subject.getExamDate() != null
                    ? subject.getExamDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : globalDate;
            String subjectTime = subject.getStartTime() != null
                    ? subject.getStartTime() + " - " + (subject.getEndTime() != null ? subject.getEndTime() : "TBD")
                    : globalTime;
            list.add(new AdmitCardDTO.SubjectScheduleDTO(subject.getSubjectName(), subjectDate, subjectTime));
        }

        if (list.isEmpty()) {
            list.add(
                    new AdmitCardDTO.SubjectScheduleDTO(firstNonBlank(exam.getExamName(), "Exam Subject"), globalDate, globalTime));
        }
        return list;
    }

    /** Returns the actual college code from the student's college, or exam.collegeId as fallback. */
    private String extractCenterCode(UniversityExam exam, User student) {
        // Primary: look up real code from student's college
        if (student != null && student.getCollege() != null) {
            String code = student.getCollege().getCode();
            if (code != null && !code.isBlank()) {
                return code.trim();
            }
        }
        // Fallback 1: look up from exam's collegeId
        if (exam.getCollegeId() != null) {
            com.example.examauth.model.College college = collegeRepository.findById(exam.getCollegeId()).orElse(null);
            if (college != null && college.getCode() != null && !college.getCode().isBlank()) {
                return college.getCode().trim();
            }
        }
        // Fallback 2: use whatever is stored on the exam itself
        String raw = exam.getCenterCode();
        if (raw == null || raw.isBlank()) return "";
        return raw.trim();
    }

    /** Returns the college name from student's college, falling back to exam.centerName. */
    private String extractCenterName(UniversityExam exam, User student) {
        // Primary: student's college
        if (student != null && student.getCollege() != null) {
            String name = student.getCollege().getName();
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
        }
        // Fallback 1: exam's collegeId
        if (exam.getCollegeId() != null) {
            com.example.examauth.model.College college = collegeRepository.findById(exam.getCollegeId()).orElse(null);
            if (college != null && college.getName() != null && !college.getName().isBlank()) {
                return college.getName().trim();
            }
        }
        return firstNonBlank(exam.getCenterName(), "Center Not Assigned");
    }

    private String buildSeatNumber(ExamRegistration registration) {
        return "S" + String.format("%08d", registration.getId());
    }

    /** PNG image as raw Base64 (no data-URL prefix) for JSON clients. */
    private String generateQrPngBase64(String payload) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, 220, 220);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate QR code");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String generateSecureHash(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
