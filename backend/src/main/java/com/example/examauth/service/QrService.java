
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

    // Optional: Autowire ExamRepository if you have one.
    // @Autowired
    // private ExamRepository examRepository;

    public com.example.examauth.dto.QrVerificationResponse verifyQr(String token) {

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

        // ✅ Mock Exam Data (Since we are focusing on Auth, and Exam entity might be minimal)
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
}
