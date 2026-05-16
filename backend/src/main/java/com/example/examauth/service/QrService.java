
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

    public com.example.examauth.dto.QrVerificationResponse verifyQr(String token) {
        if (token != null && token.startsWith("VERIFY_HASH:")) {
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
                "Final Semester Exam",
                "Advanced Java Programming",
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
            // token format: VERIFY_HASH:hash_value|REG_ID:id_value
            String[] parts = token.split("\\|");
            if (parts.length != 2)
                return new com.example.examauth.dto.QrVerificationResponse(false, "Invalid QR Format");

            String hashVal = parts[0].replace("VERIFY_HASH:", "");
            String regIdStr = parts[1].replace("REG_ID:", "");
            Long regId = Long.parseLong(regIdStr);

            var regOpt = examRegistrationRepository.findById(regId);
            if (regOpt.isEmpty())
                return new com.example.examauth.dto.QrVerificationResponse(false, "Registration not found");

            var reg = regOpt.get();

            // Re-calculate hash to verify authenticity
            String tokenData = "SECURE_EXAM_" + reg.getId() + "_" + reg.getPrn();
            String expectedHash = HashUtil.sha256(tokenData);

            // Allow matching our own Sha256 or the one generated by AdmitCardService (in
            // case they differ slightly due to util class vs local method)
            if (!hashVal.equals(expectedHash) && !hashVal.equals(generateSecureHashFallback(tokenData))) {
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

            String hallNo = "Hall " + (char) ('A' + (int) (reg.getExamId() % 5));
            String seatNo = "S" + String.format("%08d", reg.getId());

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

            response.setExam(new com.example.examauth.dto.QrVerificationResponse.ExamDetail(
                    reg.getExamId(), reg.getExamSession() != null ? reg.getExamSession() : "Semester Exam",
                    (reg.getSelectedSubjects() != null && !reg.getSelectedSubjects().isEmpty())
                            ? String.join(", ", reg.getSelectedSubjects())
                            : (reg.getCourse() != null ? reg.getCourse() : "Subject"),
                    java.time.LocalDate.now().toString(), "10:00 AM - 01:00 PM"));

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
