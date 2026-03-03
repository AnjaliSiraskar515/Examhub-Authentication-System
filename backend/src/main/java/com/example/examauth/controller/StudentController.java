package com.example.examauth.controller;

import com.example.examauth.model.User;
import com.example.examauth.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@CrossOrigin
public class StudentController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
            return ResponseEntity.status(401).body(Map.<String, Object>of("error", "Unauthorized"));
        }
        String email = authentication.getName();
        User user = userRepository.findFirstByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.<String, Object>of("error", "User not found"));
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getUserId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("phone", user.getPhoneNumber());
        profile.put("role", user.getRole());
        profile.put("lastLogin", user.getLastLogin());
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

    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody Map<String, Object> body) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String email = authentication.getName();
        User user = userRepository.findFirstByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String newName = (String) body.get("name");
        String newEmail = (String) body.get("email");
        String newPhone = (String) body.get("phoneNumber");

        if (newName != null && !newName.isEmpty()) {
            user.setName(newName);
        }
        if (newPhone != null && !newPhone.isEmpty()) {
            user.setPhoneNumber(newPhone);
        }
        if (newEmail != null && !newEmail.isEmpty() && !newEmail.equals(user.getEmail())) {
            if (userRepository.findFirstByEmail(newEmail).isPresent()) {
                return ResponseEntity.status(400).body(Map.of("error", "Email already in use"));
            }
            user.setEmail(newEmail);
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(Authentication authentication, @RequestBody Map<String, String> body) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String email = authentication.getName();
        User user = userRepository.findFirstByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Missing password fields"));
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("error", "Incorrect current password"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
