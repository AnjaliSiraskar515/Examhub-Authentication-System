package com.example.examauth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.examauth.service.AttendanceService;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.model.User;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import java.util.Base64;
import com.example.examauth.util.AESGcmUtil;

@RestController
@RequestMapping("/api/biometric")
public class BiometricController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private UserRepository userRepo;

    private static final String AES_KEY_B64 = System.getenv().getOrDefault("AES_KEY_B64", "REPLACE_BASE64_32_BYTES________________");

    @PostMapping("/register-fingerprint")
    public ResponseEntity<?> registerFingerprint(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(String.valueOf(body.get("userId")));
            String fpTemplate = (String) body.get("fingerprintTemplate");
            if (fpTemplate == null || fpTemplate.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Fingerprint missing"));
            byte[] keyBytes = Base64.getDecoder().decode(AES_KEY_B64);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            String encrypted = AESGcmUtil.encrypt(key, fpTemplate, null);
            User u = userRepo.findById(userId).orElseThrow();
            u.setBiometricHash(encrypted);
            userRepo.save(u);
            return ResponseEntity.ok(Map.of("status", "saved"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "failed_to_save"));
        }
    }

    @PostMapping("/fingerprint-verify")
    public ResponseEntity<?> verifyFingerprint(@RequestBody Map<String, Object> body) {
        try {
            Long userId = body.get("userId") != null ? Long.valueOf(String.valueOf(body.get("userId"))) : null;
            Long examId = body.get("examId") != null ? Long.valueOf(String.valueOf(body.get("examId"))) : null;
            String fpTemplate = (String) body.get("fingerprintTemplate");
            if (fpTemplate == null) return ResponseEntity.badRequest().body(Map.of("error", "no_template"));
            // fetch stored encrypted template
            User u = userRepo.findById(userId).orElse(null);
            if (u == null) return ResponseEntity.status(404).body(Map.of("error", "user_not_found"));
            String storedEnc = u.getBiometricHash();
            byte[] keyBytes = Base64.getDecoder().decode(AES_KEY_B64);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            String storedPlain = AESGcmUtil.decrypt(key, storedEnc, null);
            // naive compare (demo)
            boolean match = storedPlain != null && storedPlain.equals(fpTemplate);
            double score = match ? 0.95 : 0.12;
            if (match && examId != null) {
                attendanceService.markAttendance(examId, userId, 0L);
            } else if (!match && examId != null) {
                attendanceService.logFraud(userId, examId, "Fingerprint mismatch (admin/supervisor check)");
            }
            return ResponseEntity.ok(Map.of("match", match, "score", score));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "verify_failed"));
        }
    }
}
