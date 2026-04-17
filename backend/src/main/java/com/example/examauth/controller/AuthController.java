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

import com.example.examauth.repo.InstitutionRepository;
import com.example.examauth.repo.UserRepository; // Added import
import com.example.examauth.model.Institution; // Added import
import org.springframework.security.crypto.password.PasswordEncoder; // Added import
import com.example.examauth.service.OtpService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository; // Added UserRepository autowiring

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService; // Added OtpService autowiring

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
    // LOGIN
    // ===========================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String identifier = request.get("email"); // email or phone
            String password = request.get("password");
            String role = request.get("role");
            String prn = request.get("prn");

            // =========================================================
            // UNIVERSITY_ADMIN (Institution Admin) LOGIN LOGIC
            // =========================================================
            if ("UNIVERSITY_ADMIN".equalsIgnoreCase(role)) {
                String institutionCode = request.get("institutionCode");
                String loginKey = request.get("password"); // Send loginKey as password from frontend

                if (identifier == null || loginKey == null || institutionCode == null) {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("error", "Missing login credentials for Institution Admin");
                    return ResponseEntity.badRequest().body(resp);
                }

                // Verify OTP first!
                if (!otpService.isEmailVerified(identifier)) {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("error", "Email not verified or OTP session expired.");
                    return ResponseEntity.status(401).body(resp);
                }

                Optional<Institution> instOpt = institutionRepository.findFirstByInstitutionCode(institutionCode);

                if (instOpt.isPresent()) {
                    Institution inst = instOpt.get();
                    // Verify the email belongs to this exactly discovered institution
                    if (!identifier.equalsIgnoreCase(inst.getContactEmail())
                            && !identifier.equalsIgnoreCase(inst.getAdminEmail())) {
                        Map<String, Object> resp = new HashMap<>();
                        resp.put("error", "The provided email does not belong to this Institution Code");
                        return ResponseEntity.status(401).body(resp);
                    }
                } else {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("error", "Institution not found with this code");
                    return ResponseEntity.status(401).body(resp);
                }

                Institution institution = instOpt.get();

                if (!"approved".equalsIgnoreCase(institution.getStatus())) {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("error", "Institution is not approved yet");
                    return ResponseEntity.status(401).body(resp);
                }

                // Verify Hash using PasswordEncoder OR Plain Text for Legacy/Test Records
                String dbLoginKey = institution.getLoginKey();
                boolean isMatch = false;

                if (dbLoginKey != null && dbLoginKey.startsWith("$2a$")) {
                    isMatch = passwordEncoder.matches(loginKey, dbLoginKey);
                } else if (dbLoginKey != null) {
                    isMatch = dbLoginKey.equals(loginKey);
                }

                if (!isMatch) {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("error", "Invalid Login Key");
                    return ResponseEntity.status(401).body(resp);
                }

                // Check if user exists in User table to generate token
                Optional<User> userOpt = authService.findByEmail(identifier);
                User user;
                if (userOpt.isEmpty()) {
                    // Create minimal User record if it doesn't exist for Auth context
                    user = new User();
                    user.setEmail(identifier);
                    user.setName(institution.getAdminName());
                    user.setRole("UNIVERSITY_ADMIN");
                    user.setStatus("active");

                    // To satisfy the database NOT NULL constraint for PRN (which is only for
                    // students)
                    user.setPrn("ADM_" + java.util.UUID.randomUUID().toString().substring(0, 15));

                    // Assuming encoded password needs to be set, but not used since we verify via
                    // institution
                    user.setPassword(passwordEncoder.encode(loginKey));
                    userRepository.save(user);
                } else {
                    user = userOpt.get();
                }

                // Update Last Login
                user.setLastLogin(java.time.LocalDateTime.now());
                userRepository.save(user);

                // Consume OTP so it cannot be reused
                otpService.clearVerifiedEmailOtp(identifier);

                // ✅ Successful login response
                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "Institution Admin Login successful");
                resp.put("role", user.getRole());
                resp.put("userId", user.getUserId());
                resp.put("institutionId", institution.getInstitutionId());
                resp.put("token", jwtUtil.generateToken(user.getEmail()));

                return ResponseEntity.ok(resp);
            }
            // =========================================================

            if (identifier == null || password == null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("error", "Missing login credentials");
                return ResponseEntity.badRequest().body(resp);
            }

            // Unified identity resolution: email + role to avoid non-unique results
            Optional<User> userOpt = userRepository.findFirstByEmailAndRole(identifier, role);

            // Fallback: case-insensitive search for SUPERADMIN (handles role casing
            // mismatches in DB)
            if (userOpt.isEmpty() && (role.equalsIgnoreCase("SUPERADMIN") || role.equalsIgnoreCase("SUPER_ADMIN"))) {
                userOpt = userRepository.findByEmail(identifier)
                        .filter(u -> u.getRole() != null && (u.getRole().equalsIgnoreCase("SUPERADMIN") ||
                                u.getRole().equalsIgnoreCase("SUPER_ADMIN") ||
                                u.getRole().equalsIgnoreCase("super admin")));
            }

            if (userOpt.isEmpty()) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("error", "User not found");
                return ResponseEntity.status(401).body(resp);
            }

            User user = userOpt.get();

            // Verify role matches requested role
            if (role == null || !role.equalsIgnoreCase(user.getRole())) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("error", "Role mismatch");
                return ResponseEntity.status(401).body(resp);
            }

            // STUDENT: enforce PRN matching (unchanged)
            if ("STUDENT".equalsIgnoreCase(role)) {
                if (prn == null || prn.isBlank()) {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("error", "PRN required for students");
                    return ResponseEntity.status(401).body(resp);
                }
                if (!prn.equals(user.getPrn())) {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("error", "Invalid PRN for the given email.");
                    return ResponseEntity.status(401).body(resp);
                }
            }

            // Verify password (hashed or legacy plain)
            String dbPassword = user.getPassword();
            boolean authenticated = false;
            if (dbPassword != null && dbPassword.startsWith("$2a$")) {
                authenticated = passwordEncoder.matches(password, dbPassword);
            } else if (dbPassword != null) {
                authenticated = dbPassword.equals(password);
            }

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
