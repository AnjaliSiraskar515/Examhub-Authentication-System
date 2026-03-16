package com.example.examauth.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.examauth.util.JwtUtil;

@RestController
@CrossOrigin(origins = { "http://localhost:5500", "http://127.0.0.1:5500" })
public class TestController {

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/api/otp/test")
    public String check() {
        return "✅ Backend Connected Successfully!";
    }

    @GetMapping("/api/test/token")
    public String generateTestToken(@RequestParam(defaultValue = "test@student.com") String email) {
        return jwtUtil.generateToken(email);
    }
}
