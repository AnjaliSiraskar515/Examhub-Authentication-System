package com.example.examauth.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class TestController {

    @GetMapping("/api/otp/test")
    public String check() {
        return "✅ Backend Connected Successfully!";
    }
}
