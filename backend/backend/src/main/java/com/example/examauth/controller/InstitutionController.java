package com.example.examauth.controller;

import com.example.examauth.model.Institution;
import com.example.examauth.repo.InstitutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/institution")
public class InstitutionController {

    @Autowired
    private InstitutionRepository institutionRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerInstitution(@RequestBody Map<String, String> request) {
        try {
            Institution institution = new Institution();
            institution.setName(request.get("name"));
            institution.setAddress(request.get("address"));
            institution.setContactEmail(request.get("contactEmail"));
            institution.setContactPhone(request.get("contactPhone"));
            institution.setStatus("pending");
            institution.setCreatedOn(LocalDateTime.now());
            Institution saved = institutionRepository.save(institution);
            return ResponseEntity.ok(Map.of("message", "Institution registered successfully", "institutionId", saved.getInstitutionId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Registration failed"));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllInstitutions() {
        List<Institution> institutions = institutionRepository.findAll();
        return ResponseEntity.ok(institutions);
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveInstitution(@PathVariable Long id) {
        try {
            Institution institution = institutionRepository.findById(id).orElse(null);
            if (institution != null) {
                institution.setStatus("approved");
                institutionRepository.save(institution);
                return ResponseEntity.ok(Map.of("message", "Institution approved"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Institution not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Approval failed"));
        }
    }
}
