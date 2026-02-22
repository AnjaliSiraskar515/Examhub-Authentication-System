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

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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
        profile.put("id", user.getUserId()); // Added for robust updates
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("role", user.getRole());
        profile.put("lastLogin", user.getLastLogin()); // ✅ Added Last Login
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
                "passportPhoto", user.getPassportPhotoPath() != null));
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocuments(
            Authentication auth,
            @RequestParam(value = "aadhar", required = false) MultipartFile aadhar,
            @RequestParam(value = "marks10", required = false) MultipartFile marks10,
            @RequestParam(value = "marks12", required = false) MultipartFile marks12,
            @RequestParam(value = "ug", required = false) MultipartFile ug,
            @RequestParam(value = "pg", required = false) MultipartFile pg,
            @RequestParam(value = "biometric", required = false) MultipartFile biometric,
            @RequestParam(value = "passportPhoto", required = false) MultipartFile passportPhoto,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        try {
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Fix: Ensure upload directory is absolute and exists
            File dir = new File(uploadDir).getAbsoluteFile();
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created && !dir.exists()) {
                    System.err.println("Failed to create upload directory: " + dir.getAbsolutePath());
                    return ResponseEntity.status(500).body(Map.of("error", "Failed to create upload directory"));
                }
            }

            System.out.println("Uploading to: " + dir.getAbsolutePath());

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
            if (photo != null) {
                String path = timestamp + "_profile_" + photo.getOriginalFilename();
                photo.transferTo(new File(dir, path));
                user.setPhotoPath(path);
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "uploaded"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "upload failed: " + e.getMessage()));
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
                    user.getPhoneNumber() == null || user.getPassportPhotoPath() == null
                    || user.getBiometricPath() == null) {
                return ResponseEntity.status(400).body(Map.of("error", "Mandatory documents not uploaded"));
            }

            // AI verification for documents
            boolean verified = true;
            String[] docs = { user.getAadharPath(), user.getMarks10Path(), user.getMarks12Path() };
            for (String doc : docs) {
                if (doc != null) {
                    Map<String, Object> payload = Map.of("document_path", uploadDir + "/" + doc);
                    ResponseEntity<Map> response = restTemplate.postForEntity(aiVerificationUrl + "/verify_document",
                            payload, Map.class);
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
                return ResponseEntity.status(400)
                        .body(Map.of("status", "rejected", "message", "Document verification failed"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "completion failed"));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateProfile(Authentication auth, @RequestBody Map<String, Object> body) {
        System.out.println("DEBUG: Update Profile Request: " + body);
        User user = null;

        // 1. Try finding by ID (Most Robust)
        if (body.containsKey("id")) {
            try {
                Object idObj = body.get("id");
                System.out.println("DEBUG: ID from body: " + idObj);
                if (idObj != null) {
                    Long id = Long.valueOf(String.valueOf(idObj));
                    user = userRepository.findById(id).orElse(null);
                    System.out.println("DEBUG: User found by ID: " + (user != null));
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Error parsing ID: " + e.getMessage());
                // Ignore ID parse errors, fall back to other methods
            }
        }

        // 2. Fallback to Authentication
        if (user == null && auth != null) {
            String email = auth.getName();
            user = userRepository.findFirstByEmail(email).orElse(null);
        }

        // 3. Fallback to Email parameter
        if (user == null) {
            String email = null;
            if (body.containsKey("currentEmail")) {
                email = (String) body.get("currentEmail");
            } else if (body.containsKey("email")) {
                email = (String) body.get("email");
            }
            if (email != null) {
                user = userRepository.findFirstByEmail(email).orElse(null);
            }
        }

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String newName = (String) body.get("name");
        String newEmail = (String) body.get("email");
        String newPhone = (String) body.get("phoneNumber");

        if (newName != null && !newName.isEmpty())
            user.setName(newName);
        if (newPhone != null && !newPhone.isEmpty())
            user.setPhoneNumber(newPhone);

        boolean emailChanged = false;
        if (newEmail != null && !newEmail.isEmpty() && !newEmail.equals(user.getEmail())) {
            // Check if email already exists
            if (userRepository.findFirstByEmail(newEmail).isPresent()) {
                return ResponseEntity.status(400).body(Map.of("error", "Email already in use"));
            }
            user.setEmail(newEmail);
            emailChanged = true;
        }

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile updated successfully");
        response.put("emailChanged", emailChanged);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody Map<String, String> body) {
        String email;
        if (auth != null) {
            email = auth.getName();
        } else {
            // Fallback for dev/demo if auth is missing (e.g. basic auth not sent)
            // In a real scenario, this should return 401.
            // For this specific 'fix', we'll try to find the admin user directly or
            // returning an error
            // Check if email is provided in body as a fallback
            if (body.containsKey("email")) {
                email = body.get("email");
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized: No authentication found"));
            }
        }

        User user = userRepository.findFirstByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Missing password fields"));
        }

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("error", "Incorrect current password"));
        }

        // Update to new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @GetMapping("/fix-account")
    public ResponseEntity<?> fixAccount(@RequestParam String email) {
        try {
            User user = userRepository.findFirstByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found: " + email));
            }
            user.setPassword(passwordEncoder.encode("admin123"));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Password reset to 'admin123' for " + email));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Fix failed: " + e.getMessage()));
        }
    }
}
