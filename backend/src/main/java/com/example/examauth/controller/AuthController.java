package com.example.examauth.controller;

import com.example.examauth.model.User;
import com.example.examauth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.example.examauth.repo.UserRepository; // Added import

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository; // Added UserRepository autowiring

    @Autowired
    private AuthService authService;

    @Autowired
    private RestTemplate restTemplate; // Added RestTemplate autowiring

    @Autowired
    private com.example.examauth.util.JwtUtil jwtUtil; // Added JwtUtil autowiring

    // ===========================================================
    // REGISTER USER
    // ===========================================================
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        try {
            User user = new User();
            user.setName(request.get("name"));
            user.setEmail(request.get("email"));
            user.setPassword(request.get("password"));
            user.setRole(request.get("role"));
            user.setPhoneNumber(request.get("phone"));
            user.setIdProofPath(request.get("idProofPath"));

            // Optional username/roll number support
            if (request.containsKey("username")) {
                user.setUsername(request.get("username"));
            }

            // Map new academic fields
            if (request.containsKey("department"))
                user.setDepartment(request.get("department"));
            if (request.containsKey("major"))
                user.setMajor(request.get("major"));
            if (request.containsKey("year"))
                user.setYear(request.get("year"));

            if ("student".equalsIgnoreCase(request.get("role"))) {
                user.setStatus("active"); // directly active after OTP verification
            } else {
                user.setStatus("pending");
            }

            User savedUser = authService.registerUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", savedUser.getUserId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Registration failed");
            error.put("details", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // ===========================================================
    // LOGIN (Supports Student Login via Email OR Username)
    // ===========================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String identifier = request.get("email"); // can be email or username
            String password = request.get("password");

            if (identifier == null || password == null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("error", "Missing login credentials");
                return ResponseEntity.badRequest().body(resp);
            }

            Optional<User> userOpt = authService.findByEmail(identifier);

            // Try to find by username if not found by email
            if (userOpt.isEmpty()) {
                try {
                    userOpt = authService.findByUsername(identifier);
                } catch (Exception ignored) {
                }
            }

            // 3. Try Phone
            if (userOpt.isEmpty()) {
                try {
                    userOpt = authService.findByPhoneNumber(identifier);
                } catch (Exception ignored) {
                }
            }

            if (userOpt.isEmpty()) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("error", "User not found");
                return ResponseEntity.status(401).body(resp);
            }

            User user = userOpt.get();

            // Validate password
            boolean authenticated = authService.authenticate(user.getEmail(), password);
            if (!authenticated) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("error", "Invalid password");
                return ResponseEntity.status(401).body(resp);
            }

            // Update Last Login
            user.setLastLogin(java.time.LocalDateTime.now());
            userRepository.save(user);

            // ✅ Successful login response
            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Login successful");
            resp.put("role", user.getRole());
            resp.put("userId", user.getUserId());
            resp.put("token", jwtUtil.generateToken(user.getEmail()));

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Server error during login");
            error.put("details", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // ===========================================================
    // GET ROLE BY EMAIL
    // ===========================================================
    @GetMapping("/role/{email}")
    public ResponseEntity<?> getRole(@PathVariable String email) {
        try {
            Optional<User> user = authService.findByEmail(email);
            if (user.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("role", user.get().getRole());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "User not found");
                return ResponseEntity.status(404).body(error);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Server error");
            error.put("details", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
