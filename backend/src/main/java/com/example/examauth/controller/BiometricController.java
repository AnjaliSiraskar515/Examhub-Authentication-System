package com.example.examauth.controller;

import com.example.examauth.model.ExamAttendance;
import com.example.examauth.model.User;
import com.example.examauth.repo.ExamAttendanceRepository;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/biometric")
@CrossOrigin // Allow frontend access
public class BiometricController {

    private static final Logger log = LoggerFactory.getLogger(BiometricController.class);

    private final UserRepository userRepository;
    private final ExamAttendanceRepository examAttendanceRepository;

    public BiometricController(UserRepository userRepository, ExamAttendanceRepository examAttendanceRepository) {
        this.userRepository = userRepository;
        this.examAttendanceRepository = examAttendanceRepository;
    }

    private Map<String, Object> baseResponse(boolean success, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", success);
        m.put("message", message);
        m.put("timestamp", OffsetDateTime.now().toString());
        return m;
    }

    private static Long parseLongOrNull(Object raw) {
        if (raw == null) return null;
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void attachAttendanceFields(Map<String, Object> response,
                                        ExamAttendance attendance,
                                        boolean attendanceRecorded,
                                        boolean attendanceAlreadyRecorded) {
        response.put("attendanceRecorded", attendanceRecorded);
        response.put("attendanceAlreadyRecorded", attendanceAlreadyRecorded);

        if (attendance != null) {
            response.put("attendanceStatus", attendance.getStatus()); // PRESENT / ABSENT
            response.put("biometricResult", attendance.getBiometricResult()); // SUCCESS / FAILED
            response.put("verifiedAt", attendance.getVerifiedAt() != null ? attendance.getVerifiedAt().toString() : null);
            response.put("examId", attendance.getExamId());
            response.put("studentId", attendance.getStudentId());
            response.put("supervisorId", attendance.getSupervisorId());
        }
    }

    private static final class AttendanceWriteResult {
        private final ExamAttendance attendance;
        private final boolean recorded;
        private final boolean alreadyRecorded;

        private AttendanceWriteResult(ExamAttendance attendance, boolean recorded, boolean alreadyRecorded) {
            this.attendance = attendance;
            this.recorded = recorded;
            this.alreadyRecorded = alreadyRecorded;
        }
    }

    private AttendanceWriteResult recordAttendanceIfPossible(Long studentId,
                                                             Long examId,
                                                             Long supervisorId,
                                                             String status,
                                                             String biometricResult,
                                                             LocalDateTime verifiedAt) {
        if (examId == null) {
            return new AttendanceWriteResult(null, false, false);
        }

        var existingOpt = examAttendanceRepository.findFirstByStudentIdAndExamId(studentId, examId);
        if (existingOpt.isPresent()) {
            return new AttendanceWriteResult(existingOpt.get(), false, true);
        }

        ExamAttendance attendance = new ExamAttendance();
        attendance.setStudentId(studentId);
        attendance.setExamId(examId);
        attendance.setSupervisorId(supervisorId);
        attendance.setStatus(status);
        attendance.setBiometricResult(biometricResult);
        attendance.setVerifiedAt(verifiedAt);
        ExamAttendance saved = examAttendanceRepository.save(attendance);
        return new AttendanceWriteResult(saved, true, false);
    }

    @PostMapping("/capture")
    public ResponseEntity<?> captureFingerprint(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
            return ResponseEntity.status(401).body(baseResponse(false, "Unauthorized"));
        }

        String operatorEmail = authentication.getName();
        log.info("Biometric capture requested by operator {}", operatorEmail);

        // Simulate interacting with a biometric device
        Map<String, Object> response = baseResponse(true, "Fingerprint captured successfully from device.");
        response.put("hash", "bio_sample_" + UUID.randomUUID().toString().substring(0, 16));
        return ResponseEntity.ok(response);
    }

    /**
     * Verify (and lazily enroll) student biometric.
     *
     * Expects JSON body: { "studentId": "...", "fingerprint": "raw-or-base64" }
     * The authenticated user (typically supervisor) is taken from JWT but the
     * biometric template is stored on the target student.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyBiometric(Authentication authentication, @RequestBody Map<String, Object> req) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
            return ResponseEntity.status(401).body(baseResponse(false, "Unauthorized"));
        }

        String operatorEmail = authentication.getName();

        Object studentIdRaw = req.get("studentId");
        Object fingerprintRaw = req.get("fingerprint");
        Object examIdRaw = req.get("examId");

        if (studentIdRaw == null || fingerprintRaw == null) {
            return ResponseEntity.badRequest()
                    .body(baseResponse(false, "Missing studentId or fingerprint payload"));
        }

        // Ignore invalid examId - attendance will simply not be recorded
        Long examId = parseLongOrNull(examIdRaw);

        Long studentId;
        try {
            studentId = Long.parseLong(studentIdRaw.toString());
        } catch (NumberFormatException ex) {
            return ResponseEntity.badRequest()
                    .body(baseResponse(false, "Invalid studentId format"));
        }

        String fingerprint = fingerprintRaw.toString();
        if (fingerprint == null || fingerprint.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(baseResponse(false, "Empty fingerprint payload"));
        }

        String templateHash = HashUtil.sha256(fingerprint);

        return userRepository.findById(studentId).map((User user) -> {
            String userEmail = user.getEmail();
            LocalDateTime now = LocalDateTime.now();

            String existingHash = user.getBiometricTemplateHash();

            // Resolve supervisor (operator) id for attendance marking (if possible)
            Long supervisorId = userRepository.findFirstByEmail(operatorEmail)
                    .map(User::getUserId)
                    .orElse(null);

            // Enrollment
            if (existingHash == null || existingHash.isEmpty()) {
                user.setBiometricTemplateHash(templateHash);
                user.setBiometricEnrolled(true);
                user.setBiometricEnrolledAt(now);
                user.setBiometricLastVerified(now);
                user.setBiometricVerified(true); // keep legacy flag in sync
                userRepository.save(user);

                AttendanceWriteResult attendanceResult = recordAttendanceIfPossible(
                        studentId,
                        examId,
                        supervisorId,
                        "PRESENT",
                        "SUCCESS",
                        now
                );

                log.info("Biometric enrolled for user {} by operator {} at {}", userEmail, operatorEmail, now);

                Map<String, Object> response = baseResponse(true, "Biometric enrolled and verified for " + user.getName());
                response.put("score", 100.0);
                attachAttendanceFields(response, attendanceResult.attendance, attendanceResult.recorded, attendanceResult.alreadyRecorded);
                if (attendanceResult.alreadyRecorded) {
                    response.put("message", "Attendance already recorded.");
                    return ResponseEntity.ok(response);
                }
                return ResponseEntity.ok(response);
            }

            // Verification
            if (existingHash.equals(templateHash)) {
                user.setBiometricLastVerified(now);
                user.setBiometricVerified(true);
                userRepository.save(user);

                AttendanceWriteResult attendanceResult = recordAttendanceIfPossible(
                        studentId,
                        examId,
                        supervisorId,
                        "PRESENT",
                        "SUCCESS",
                        now
                );

                log.info("Biometric verified successfully for user {} by operator {} at {}", userEmail, operatorEmail,
                        now);

                Map<String, Object> response = baseResponse(true, "Biometric Match Confirmed: " + user.getName());
                response.put("score", 98.5); // simulated confidence
                attachAttendanceFields(response, attendanceResult.attendance, attendanceResult.recorded, attendanceResult.alreadyRecorded);
                if (attendanceResult.alreadyRecorded) {
                    response.put("message", "Attendance already recorded.");
                    return ResponseEntity.ok(response);
                }
                return ResponseEntity.ok(response);
            }

            // Mismatch - record as absent (FAILED)
            user.setBiometricVerified(false);
            userRepository.save(user);

            log.warn("Biometric verification FAILED for user {} by operator {} at {}", userEmail, operatorEmail, now);

            AttendanceWriteResult attendanceResult = recordAttendanceIfPossible(
                    studentId,
                    examId,
                    supervisorId,
                    "ABSENT",
                    "FAILED",
                    now
            );

            Map<String, Object> response = baseResponse(false, "Biometric verification failed");
            attachAttendanceFields(response, attendanceResult.attendance, attendanceResult.recorded, attendanceResult.alreadyRecorded);
            if (attendanceResult.alreadyRecorded) {
                response.put("message", "Attendance already recorded.");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(401).body(response);
        }).orElseGet(() -> {
            log.warn("Biometric verification requested for unknown studentId {} by operator {}", studentId, operatorEmail);
            return ResponseEntity.status(404).body(baseResponse(false, "Student not found"));
        });
    }
}
