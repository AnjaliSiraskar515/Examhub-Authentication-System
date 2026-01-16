package com.example.examauth.controller;

import com.example.examauth.model.User;
import com.example.examauth.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${ai.verification.url:http://localhost:5001}")
    private String aiVerificationUrl;

    @GetMapping("/info")
    public ResponseEntity<?> getProfileInfo(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("role", user.getRole());
        profile.put("photoPath", user.getPhotoPath());
        profile.put("passportPhotoPath", user.getPassportPhotoPath());
        profile.put("profileCompleted", user.getProfileCompleted());
        profile.put("documents", Map.of(
            "aadhar", user.getAadharPath() != null,
            "marks10", user.getMarks10Path() != null,
            "marks12", user.getMarks12Path() != null,
            "ug", user.getUgPath() != null,
            "pg", user.getPgPath() != null,
            "biometric", user.getBiometricPath() != null,
            "passportPhoto", user.getPassportPhotoPath() != null
        ));
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocuments(
            Authentication auth,
            @RequestParam(value="aadhar", required=false) MultipartFile aadhar,
            @RequestParam(value="marks10", required=false) MultipartFile marks10,
            @RequestParam(value="marks12", required=false) MultipartFile marks12,
            @RequestParam(value="ug", required=false) MultipartFile ug,
            @RequestParam(value="pg", required=false) MultipartFile pg,
            @RequestParam(value="biometric", required=false) MultipartFile biometric,
            @RequestParam(value="passportPhoto", required=false) MultipartFile passportPhoto) {
        try {
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String timestamp = String.valueOf(System.currentTimeMillis());

            if (aadhar != null) {
                String path = timestamp + "_" + aadhar.getOriginalFilename();
                aadhar.transferTo(new File(dir, path));
                user.setAadharPath(path);
            }
            if (marks10 != null) {
                String path = timestamp + "_" + marks10.getOriginalFilename();
                marks10.transferTo(new File(dir, path));
                user.setMarks10Path(path);
            }
            if (marks12 != null) {
                String path = timestamp + "_" + marks12.getOriginalFilename();
                marks12.transferTo(new File(dir, path));
                user.setMarks12Path(path);
            }
            if (ug != null) {
                String path = timestamp + "_" + ug.getOriginalFilename();
                ug.transferTo(new File(dir, path));
                user.setUgPath(path);
            }
            if (pg != null) {
                String path = timestamp + "_" + pg.getOriginalFilename();
                pg.transferTo(new File(dir, path));
                user.setPgPath(path);
            }
            if (biometric != null) {
                String path = timestamp + "_" + biometric.getOriginalFilename();
                biometric.transferTo(new File(dir, path));
                user.setBiometricPath(path);
            }
            if (passportPhoto != null) {
                String path = timestamp + "_" + passportPhoto.getOriginalFilename();
                passportPhoto.transferTo(new File(dir, path));
                user.setPassportPhotoPath(path);
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "uploaded"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "upload failed"));
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<?> completeProfile(Authentication auth) {
        try {
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Check mandatory documents
            if (user.getAadharPath() == null || user.getMarks10Path() == null || user.getMarks12Path() == null ||
                user.getPhoneNumber() == null || user.getPassportPhotoPath() == null || user.getBiometricPath() == null) {
                return ResponseEntity.status(400).body(Map.of("error", "Mandatory documents not uploaded"));
            }

            // AI verification for documents
            boolean verified = true;
            String[] docs = {user.getAadharPath(), user.getMarks10Path(), user.getMarks12Path()};
            for (String doc : docs) {
                if (doc != null) {
                    Map<String, Object> payload = Map.of("document_path", uploadDir + "/" + doc);
                    ResponseEntity<Map> response = restTemplate.postForEntity(aiVerificationUrl + "/verify_document", payload, Map.class);
                    if (response.getBody() != null && !"verified".equals(response.getBody().get("status"))) {
                        verified = false;
                        break;
                    }
                }
            }

            if (verified) {
                user.setProfileCompleted(true);
                user.setStatus("active");
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("status", "approved", "message", "Profile completed and verified"));
            } else {
                return ResponseEntity.status(400).body(Map.of("status", "rejected", "message", "Document verification failed"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "completion failed"));
        }
    }
}
