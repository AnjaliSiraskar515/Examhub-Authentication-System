package com.example.examauth.controller;

import com.example.examauth.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500"})
public class OtpController {

    private final OtpService otpService;

    // Constructor injection avoids null/autowire problems at compile/runtime
    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/send/email")
    public ResponseEntity<Map<String, Object>> sendEmailOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        boolean sent = otpService.sendEmailOtp(email);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", sent);
        resp.put("message", sent ? "OTP sent successfully" : "Failed to send OTP");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/send/phone")
    public ResponseEntity<Map<String, Object>> sendPhoneOtp(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        boolean sent = otpService.sendPhoneOtp(phone);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", sent);
        resp.put("message", sent ? "OTP sent successfully" : "Failed to send OTP");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verify/email")
    public ResponseEntity<Map<String, Object>> verifyEmailOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        boolean verified = otpService.verifyEmailOtp(email, otp);
        Map<String, Object> resp = new HashMap<>();
        resp.put("verified", verified);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verify/phone")
    public ResponseEntity<Map<String, Object>> verifyPhoneOtp(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String otp = body.get("otp");
        boolean verified = otpService.verifyPhoneOtp(phone, otp);
        Map<String, Object> resp = new HashMap<>();
        resp.put("verified", verified);
        return ResponseEntity.ok(resp);
    }
}
