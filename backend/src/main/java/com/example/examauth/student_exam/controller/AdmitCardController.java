package com.example.examauth.student_exam.controller;

import com.example.examauth.student_exam.dto.AdmitCardDTO;
import com.example.examauth.student_exam.service.AdmitCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admit-card")
@RequiredArgsConstructor
public class AdmitCardController {

    private final AdmitCardService admitCardService;

    @GetMapping("/{registrationId}")
    public ResponseEntity<AdmitCardDTO> getAdmitCard(@PathVariable Long registrationId) {
        return ResponseEntity.ok(admitCardService.getAdmitCard(registrationId));
    }
}
