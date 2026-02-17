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
            institution.setInstitutionCode(request.get("institutionCode"));
            institution.setAddress(request.get("address"));
            institution.setContactEmail(request.get("contactEmail"));
            institution.setContactPhone(request.get("contactPhone"));
            institution.setAdminName(request.get("adminName"));
            institution.setAdminEmail(request.get("adminEmail"));
            institution.setStatus("pending");
            institution.setCreatedOn(LocalDateTime.now());
            Institution saved = institutionRepository.save(institution);
            return ResponseEntity.ok(Map.of("message", "Institution registered successfully", "institutionId",
                    saved.getInstitutionId()));
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

    @Autowired
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String senderEmail;

    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveInstitution(@PathVariable Long id) {
        try {
            Institution institution = institutionRepository.findById(id).orElse(null);
            if (institution != null) {
                institution.setStatus("approved");

                // Generate Unique Login Key
                String loginKey = "INST-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                institution.setLoginKey(loginKey);

                institutionRepository.save(institution);

                // Send Email Notification
                sendApprovalEmail(institution);

                return ResponseEntity.ok(Map.of("message", "Institution approved"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Institution not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Approval failed"));
        }
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectInstitution(@PathVariable Long id) {
        try {
            Institution institution = institutionRepository.findById(id).orElse(null);
            if (institution != null) {
                institution.setStatus("rejected");
                institutionRepository.save(institution);

                // Send Rejection Email
                sendRejectionEmail(institution);

                return ResponseEntity.ok(Map.of("message", "Institution rejected"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Institution not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Rejection failed"));
        }
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<?> removeInstitution(@PathVariable Long id) {
        try {
            if (institutionRepository.existsById(id)) {
                institutionRepository.deleteById(id);
                return ResponseEntity.ok(Map.of("message", "Institution removed"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Institution not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Removal failed"));
        }
    }

    private void sendApprovalEmail(Institution institution) {
        try {
            if (institution.getContactEmail() == null || institution.getContactEmail().isEmpty())
                return;

            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(institution.getContactEmail());
            message.setFrom(senderEmail);
            message.setSubject("Institution Approval Notification - ExamHub");
            message.setText("Dear " + institution.getAdminName() + ",\n\n" +
                    "Congratulations! Your institution '" + institution.getName() + "' has been approved.\n\n" +
                    "Here is your Unique Login Key: " + institution.getLoginKey() + "\n\n" +
                    "Please use this key to access your institution dashboard.\n" +
                    "Do not share this key with unauthorized personnel.\n\n" +
                    "Best Regards,\nExamHub Team");

            mailSender.send(message);
            System.out.println("✅ Approval email sent to: " + institution.getContactEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to send approval email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendRejectionEmail(Institution institution) {
        try {
            if (institution.getContactEmail() == null || institution.getContactEmail().isEmpty())
                return;

            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(institution.getContactEmail());
            message.setFrom(senderEmail);
            message.setSubject("Institution Application Status - ExamHub");
            message.setText("Dear " + institution.getAdminName() + ",\n\n" +
                    "We regret to inform you that your application for '" + institution.getName()
                    + "' has been declined at this time.\n\n" +
                    "If you believe this is an error or wish to re-apply with corrected details, please contact our support team.\n\n"
                    +
                    "Best Regards,\nExamHub Team");

            mailSender.send(message);
            System.out.println("⚠️ Rejection email sent to: " + institution.getContactEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to send rejection email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
