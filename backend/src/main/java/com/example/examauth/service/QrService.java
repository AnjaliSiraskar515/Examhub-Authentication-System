
package com.example.examauth.service;

import com.example.examauth.model.QrCode;
import com.example.examauth.repo.QrRepository;
import com.example.examauth.util.HashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class QrService {

    private static final String SECRET = "EXAMHUB_SECRET_KEY"; // In production, move to properties!

    @Autowired
    private QrRepository qrRepository;

    public String generateQrToken(Long studentId, Long examId) {

        String rawData = studentId + "|" + examId + "|" + System.currentTimeMillis() + "|" + SECRET;
        String hash = HashUtil.sha256(rawData);

        QrCode qr = new QrCode();
        qr.setStudentId(studentId);
        qr.setExamId(examId);
        qr.setHashedToken(hash);
        qr.setIssuedAt(LocalDateTime.now());
        qr.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // 10 minutes expiry

        qrRepository.save(qr);

        return hash;
    }

    @Autowired
    private com.example.examauth.repo.UserRepository userRepository;

    @Autowired
    private com.example.examauth.student_exam.repo.ExamRegistrationRepository examRegistrationRepository;

    @Autowired
    private com.example.examauth.student_exam.repo.ExamSeatAllocationRepository examSeatAllocationRepository;

    @Autowired
    private com.example.examauth.student_exam.university.repo.UniversityExamRepository universityExamRepository;

    public com.example.examauth.dto.QrVerificationResponse verifyQr(String token) {
        if (token != null && (token.startsWith("VERIFY_HASH:") || token.startsWith("EXAMHUB-"))) {
            return verifyAdmitCardQr(token);
        }

        var qrOpt = qrRepository.findByHashedToken(token);

        if (qrOpt.isEmpty()) {
            return new com.example.examauth.dto.QrVerificationResponse(false, "Token not found");
        }

        QrCode qr = qrOpt.get();

        if (qr.isUsed()) {
            return new com.example.examauth.dto.QrVerificationResponse(false, "Token already used");
        }

        if (qr.getExpiresAt().isBefore(LocalDateTime.now())) {
            return new com.example.examauth.dto.QrVerificationResponse(false, "Token expired");
        }

        // ✅ Mark as used (single-use QR)
        qr.setUsed(true);
        qrRepository.save(qr);

        // ✅ Fetch Student Data
        var userOpt = userRepository.findById(qr.getStudentId());
        if (userOpt.isEmpty()) {
            return new com.example.examauth.dto.QrVerificationResponse(false, "Student record not found");
        }

        com.example.examauth.model.User user = userOpt.get();
        String maskedAadhar = (user.getAadharPath() != null && user.getAadharPath().length() > 4)
                ? "XXXX-XXXX-" + user.getAadharPath().substring(user.getAadharPath().length() - 4) // Simplified masking
                                                                                                   // logic (using
                                                                                                   // path/id string for
                                                                                                   // demo)
                : "XXXX-XXXX-1234";

        // --- Biometric status snapshot for UI (no gating here) ---
        java.time.LocalDateTime lastVerified = user.getBiometricLastVerified();
        boolean biometricEnrolled = user.isBiometricEnrolled();
        boolean biometricRecentlyVerified = biometricEnrolled
                && lastVerified != null
                && !lastVerified.isBefore(LocalDateTime.now().minusMinutes(5));

        // Derive hall / seat numbers deterministically for display
        String hallNo = "Hall " + (char) ('A' + (int) (qr.getExamId() % 5));
        String seatNo = "Seat " + String.format("%02d", (user.getUserId() % 60) + 1);

        com.example.examauth.dto.QrVerificationResponse response = new com.example.examauth.dto.QrVerificationResponse(
                true, "QR verified");

        response.setStudent(new com.example.examauth.dto.QrVerificationResponse.StudentDetail(
                user.getUserId(),
                user.getName(),
                user.getPrn() != null ? user.getPrn() : "ROLL-" + user.getUserId(),
                user.getPrn(),
                user.getPhotoPath() != null ? user.getPhotoPath() : "/placeholder-avatar.png",
                maskedAadhar,
                hallNo,
                seatNo));

        // ✅ Mock Exam Data (Since we are focusing on Auth, and Exam entity might be
        // minimal)
        // In a real scenario, you would do: examRepository.findById(qr.getExamId())
        response.setExam(new com.example.examauth.dto.QrVerificationResponse.ExamDetail(
                qr.getExamId(),
                null,
                "Final Semester Exam",
                "Advanced Java Programming",
                "CS-501",
                java.time.LocalDate.now().toString(),
                "10:00 AM - 01:00 PM"));

        response.setBiometricEnrolled(biometricEnrolled);
        response.setBiometricRecentlyVerified(biometricRecentlyVerified);

        response.setInstitution(new com.example.examauth.dto.QrVerificationResponse.InstitutionDetail(
                "ExamHub University",
                "CENTER-001"));

        return response;
    }

    private com.example.examauth.dto.QrVerificationResponse verifyAdmitCardQr(String token) {
        try {
            Long regId;
            String hashVal;

            if (token.startsWith("EXAMHUB-")) {
                // format: EXAMHUB-regId-hash
                String[] parts = token.split("-");
                if (parts.length != 3) return new com.example.examauth.dto.QrVerificationResponse(false, "Invalid QR Format");
                regId = Long.parseLong(parts[1]);
                hashVal = parts[2];
            } else {
                // token format: VERIFY_HASH:hash_value|REG_ID:id_value
                String[] parts = token.split("\\|");
                if (parts.length != 2)
                    return new com.example.examauth.dto.QrVerificationResponse(false, "Invalid QR Format");

                hashVal = parts[0].replace("VERIFY_HASH:", "");
                String regIdStr = parts[1].replace("REG_ID:", "");
                regId = Long.parseLong(regIdStr);
            }

            var regOpt = examRegistrationRepository.findById(regId);
            if (regOpt.isEmpty())
                return new com.example.examauth.dto.QrVerificationResponse(false, "Registration not found");

            var reg = regOpt.get();

            // Re-calculate hash to verify authenticity
            String tokenData = "SECURE_EXAM_" + reg.getId() + "_" + reg.getPrn();
            String expectedHash = HashUtil.sha256(tokenData);

            boolean isMatch = false;
            if (token.startsWith("EXAMHUB-")) {
                String expectedPrefix = expectedHash.substring(0, 16).toUpperCase();
                String fallbackPrefix = generateSecureHashFallback(tokenData).substring(0, 16).toUpperCase();
                isMatch = expectedPrefix.equals(hashVal) || fallbackPrefix.equals(hashVal);
            } else {
                isMatch = hashVal.equals(expectedHash) || hashVal.equals(generateSecureHashFallback(tokenData));
            }

            if (!isMatch) {
                return new com.example.examauth.dto.QrVerificationResponse(false,
                        "Security Hash Mismatch (Tampered QR)");
            }

            // Valid static QR! Fetch User
            var userOpt = userRepository.findById(reg.getStudentId());
            if (userOpt.isEmpty())
                return new com.example.examauth.dto.QrVerificationResponse(false, "Student not found");

            com.example.examauth.model.User user = userOpt.get();
            String maskedAadhar = "XXXX-XXXX-" + (user.getAadharPath() != null && user.getAadharPath().length() > 4
                    ? user.getAadharPath().substring(user.getAadharPath().length() - 4)
                    : "1234");

            java.time.LocalDateTime lastVerified = user.getBiometricLastVerified();
            boolean biometricEnrolled = user.isBiometricEnrolled();
            boolean biometricRecentlyVerified = biometricEnrolled && lastVerified != null
                    && !lastVerified.isBefore(LocalDateTime.now().minusMinutes(5));

            var seatAllocOpt = examSeatAllocationRepository.findFirstByRegistrationId(reg.getId());
            String hallNo = "Not Assigned";
            String seatNo = "Not Assigned";
            if (seatAllocOpt.isPresent()) {
                var seatAlloc = seatAllocOpt.get();
                if (seatAlloc.getHallName() != null) hallNo = seatAlloc.getHallName();
                if (seatAlloc.getSeatNumber() != null) seatNo = seatAlloc.getSeatNumber();
            }

            String photoUrl = "/placeholder-avatar.png";
            if (user.getPassportPhotoPath() != null && !user.getPassportPhotoPath().trim().isEmpty()) {
                String path = user.getPassportPhotoPath().replace("\\", "/");
                photoUrl = path.startsWith("uploads") ? "/" + path : "/uploads/" + path;
            } else if (user.getPhotoPath() != null && !user.getPhotoPath().trim().isEmpty()) {
                String path = user.getPhotoPath().replace("\\", "/");
                photoUrl = path.startsWith("/") ? path : "/" + path;
            }

            com.example.examauth.dto.QrVerificationResponse response = new com.example.examauth.dto.QrVerificationResponse(
                    true, "Admit Card Verified");
            com.example.examauth.dto.QrVerificationResponse.StudentDetail sd = new com.example.examauth.dto.QrVerificationResponse.StudentDetail(
                    user.getUserId(), user.getName(), reg.getPrn(), reg.getPrn(),
                    photoUrl,
                    maskedAadhar, hallNo, seatNo);

            String dept = user.getDepartment() != null ? user.getDepartment() : "BE";
            String major = user.getMajor() != null ? user.getMajor() : "Computer Science";
            sd.setCourseInfo(dept + " - " + major);
            response.setStudent(sd);

            String finalSubjectName = "No Exams Today";
            String finalSubjectCode = "N/A";
            String finalTime = "--";
            Long bestSubjectId = null;

            var uExamOpt = universityExamRepository.findById(reg.getExamId());
            if (uExamOpt.isPresent()) {
                var uExam = uExamOpt.get();
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalTime nowTime = java.time.LocalTime.now();
                com.example.examauth.student_exam.university.model.UniversityExamSubject bestSubject = null;

                if (uExam.getSubjects() != null) {
                    for (var sub : uExam.getSubjects()) {
                        if (reg.getSelectedSubjects() != null && reg.getSelectedSubjects().contains(sub.getSubjectName())) {
                            if (sub.getExamDate() != null && sub.getExamDate().equals(today)) {
                                if (bestSubject == null) {
                                    bestSubject = sub;
                                } else {
                                    if (sub.getStartTime() != null && bestSubject.getStartTime() != null) {
                                        long diff1 = Math.abs(java.time.Duration.between(nowTime, sub.getStartTime()).toMinutes());
                                        long diff2 = Math.abs(java.time.Duration.between(nowTime, bestSubject.getStartTime()).toMinutes());
                                        if (diff1 < diff2) {
                                            bestSubject = sub;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (bestSubject != null) {
                    finalSubjectName = bestSubject.getSubjectName();
                    finalSubjectCode = bestSubject.getSubjectCode() != null ? bestSubject.getSubjectCode() : "N/A";
                    finalTime = (bestSubject.getStartTime() != null ? bestSubject.getStartTime().toString() : "") +
                            (bestSubject.getEndTime() != null ? " - " + bestSubject.getEndTime().toString() : "");
                    bestSubjectId = bestSubject.getId();
                }
            }

            response.setExam(new com.example.examauth.dto.QrVerificationResponse.ExamDetail(
                    reg.getExamId(), bestSubjectId, reg.getExamSession() != null ? reg.getExamSession() : "Semester Exam",
                    finalSubjectName,
                    finalSubjectCode,
                    java.time.LocalDate.now().toString(), finalTime));

            response.setBiometricEnrolled(biometricEnrolled);
            response.setBiometricRecentlyVerified(biometricRecentlyVerified);
            response.setInstitution(new com.example.examauth.dto.QrVerificationResponse.InstitutionDetail(
                    "ExamHub University", "CENTER-001"));
            return response;
        } catch (Exception e) {
            return new com.example.examauth.dto.QrVerificationResponse(false, "Failed to parse Admit Card QR");
        }
    }

    private String generateSecureHashFallback(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
