package com.example.examauth.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/biometric")
@CrossOrigin // Allow frontend access
public class BiometricController {

    @PostMapping("/capture")
    public Map<String, Object> captureFingerprint() {
        // Mock biometric capture
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Fingerprint captured successfully");
        response.put("hash", "biometric_hash_" + System.currentTimeMillis());
        return response;
    }

    @PostMapping("/verify")
    public Map<String, Object> verifyBiometric(@RequestBody Map<String, Object> req) {
        // Mock verification
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Fingerprint verified (Mock)");
        response.put("score", 98.5);
        return response;
    }
}
