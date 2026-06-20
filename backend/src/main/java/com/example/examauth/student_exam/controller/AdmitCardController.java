package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.AdmitCardDTO;
import com.example.examauth.student_exam.model.ExamRegistration;
import com.example.examauth.student_exam.model.ExamSeatAllocation;
import com.example.examauth.student_exam.repo.ExamRegistrationRepository;
import com.example.examauth.student_exam.repo.ExamSeatAllocationRepository;
import com.example.examauth.student_exam.service.AdmitCardService;
import com.example.examauth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admit-card")
@RequiredArgsConstructor
public class AdmitCardController {

    private final AdmitCardService admitCardService;
    private final ExamRegistrationRepository examRegistrationRepository;
    private final ExamSeatAllocationRepository examSeatAllocationRepository;
    private final UserRepository userRepository;
    private final com.example.examauth.student_exam.university.repo.UniversityExamRepository universityExamRepository;

    /** Student-facing: get full admit card by registrationId */
    @GetMapping("/{registrationId}")
    public ResponseEntity<AdmitCardDTO> getAdmitCard(@PathVariable Long registrationId) {
        return ResponseEntity.ok(admitCardService.getAdmitCard(registrationId));
    }

    /**
     * Supervisor-only endpoint: resolve hall + seat + eligibility from opaque QR token.
     * QR token format: EXAMHUB-{registrationId}-{hashPrefix}
     * Only SUPERVISOR role can call this — students cannot discover hall via QR scan.
     */
    @GetMapping("/verify/{token}")
    @PreAuthorize("hasAnyRole('SUPERVISOR','UNIVERSITY_ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyQrToken(@PathVariable String token) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Parse token: EXAMHUB-{regId}-{hashPrefix}
        if (token == null || !token.startsWith("EXAMHUB-")) {
            result.put("valid", false);
            result.put("message", "Invalid QR token format");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            String[] parts = token.split("-");
            if (parts.length < 3) throw new IllegalArgumentException("Malformed token");
            Long registrationId = Long.parseLong(parts[1]);

            ExamRegistration reg = examRegistrationRepository.findById(registrationId)
                    .orElseThrow(() -> new IllegalArgumentException("Registration not found"));

            // Validate hash prefix
            String tokenData = "SECURE_EXAM_" + reg.getId() + "_" + reg.getPrn();
            String expectedHash = generateSecureHash(tokenData).substring(0, 16).toUpperCase();
            String providedHash = parts[2];

            if (!expectedHash.equals(providedHash)) {
                result.put("valid", false);
                result.put("message", "Token verification failed — possible tampering");
                return ResponseEntity.status(403).body(result);
            }

            // Fetch student info
            com.example.examauth.model.User student = userRepository.findById(reg.getStudentId()).orElse(null);

            // Fetch seat allocation
            ExamSeatAllocation seat = examSeatAllocationRepository
                    .findFirstByRegistrationId(registrationId).orElse(null);

            // Fetch UniversityExam to find today's subject slot
            com.example.examauth.student_exam.university.model.UniversityExam exam = 
                universityExamRepository.findById(reg.getExamId()).orElse(null);

            String todaySubjectName = "No exam scheduled for today";
            boolean hasExamToday = false;
            if (exam != null && exam.getSubjects() != null) {
                java.time.LocalDate today = java.time.LocalDate.now();
                for (com.example.examauth.student_exam.university.model.UniversityExamSubject sub : exam.getSubjects()) {
                    if (sub.getExamDate() != null && sub.getExamDate().isEqual(today)) {
                        todaySubjectName = sub.getSubjectName();
                        hasExamToday = true;
                        break;
                    }
                }
            }

            result.put("valid", true);
            result.put("registrationId", reg.getId());
            result.put("studentName", reg.getFullName() != null ? reg.getFullName() : (student != null ? student.getName() : "N/A"));
            result.put("prn", reg.getPrn());
            result.put("rollNumber", seat != null ? seat.getRollNumber() : "N/A");
            result.put("course", reg.getCourse());
            result.put("examStatus", reg.getRegistrationStatus().name());
            result.put("eligibility", reg.getRegistrationStatus() == ExamRegistration.RegistrationStatus.APPROVED ? "ELIGIBLE" : "NOT ELIGIBLE");
            
            // Security: Only reveal Hall/Seat if they have an exam today
            if (hasExamToday) {
                result.put("hallName", seat != null && seat.getHallName() != null ? seat.getHallName() : "Not Assigned");
                result.put("seatNumber", seat != null ? seat.getSeatNumber() : "Not Assigned");
                result.put("todaySubject", todaySubjectName);
            } else {
                result.put("hallName", "Hidden (No Exam Today)");
                result.put("seatNumber", "Hidden");
                result.put("todaySubject", todaySubjectName);
            }
            
            result.put("paymentStatus", reg.getPaymentStatus() != null ? reg.getPaymentStatus().name() : "PENDING");

        } catch (NumberFormatException e) {
            result.put("valid", false);
            result.put("message", "Invalid registration ID in token");
            return ResponseEntity.badRequest().body(result);
        } catch (IllegalArgumentException e) {
            result.put("valid", false);
            result.put("message", e.getMessage());
            return ResponseEntity.status(404).body(result);
        }

        return ResponseEntity.ok(result);
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
