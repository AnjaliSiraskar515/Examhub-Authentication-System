package com.example.examauth.controller;

import com.example.examauth.service.QrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qr")
@CrossOrigin // Allow frontend access
public class QrController {

    @Autowired
    private QrService qrService;

    @PostMapping("/generate")
    public Map<String, String> generate(@RequestBody Map<String, Object> req) {
        try {
            Object sIdObj = req.get("studentId");
            Object eIdObj = req.get("examId");

            if (sIdObj == null || eIdObj == null) {
                throw new IllegalArgumentException("Missing studentId or examId");
            }

            Long studentId = sIdObj instanceof Number ? ((Number) sIdObj).longValue()
                    : Long.parseLong(sIdObj.toString());
            Long examId = eIdObj instanceof Number ? ((Number) eIdObj).longValue() : Long.parseLong(eIdObj.toString());

            String token = qrService.generateQrToken(studentId, examId);
            return Map.of("qrToken", token);
        } catch (Exception e) {
            e.printStackTrace(); // Log error for debugging
            throw new RuntimeException("Error generating QR: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public com.example.examauth.dto.QrVerificationResponse verify(@RequestBody Map<String, String> req) {
        return qrService.verifyQr(req.get("qrToken"));
    }
}