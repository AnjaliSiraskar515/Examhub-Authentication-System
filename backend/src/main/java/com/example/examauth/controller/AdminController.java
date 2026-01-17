package com.example.examauth.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.model.User;
import com.example.examauth.repo.QRCodeRepository;
import com.example.examauth.model.QRCodeEntry;
import com.example.examauth.repo.FraudLogRepository;
import com.example.examauth.model.FraudLog;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private QRCodeRepository qrRepo;

    @Autowired
    private FraudLogRepository fraudRepo;

    @GetMapping("/summary")
    public ResponseEntity<?> summary() {
        long users = userRepo.count();
        long qrs = qrRepo.count();
        long frauds = fraudRepo.count();
        return ResponseEntity.ok(Map.of("users", users, "qrs", qrs, "frauds", frauds));
    }

    @GetMapping("/supervisors")
    public ResponseEntity<?> getSupervisors() {
        List<Map<String, Object>> s = userRepo.findAll().stream()
                .filter(u -> "SUPERVISOR".equalsIgnoreCase(u.getRole()))
                .map(u -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("userId", u.getUserId());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("status", u.getStatus());
                    map.put("role", u.getRole());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(s);
    }

    @PostMapping("/assign-supervisor")
    public ResponseEntity<?> assignSupervisor(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        // create or find supervisor user
        User sup = userRepo.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName(email.split("@")[0]);
            u.setRole("SUPERVISOR");
            u.setStatus("active");
            u.setPassword("temp"); // In production, send invite email
            return userRepo.save(u);
        });
        // In production create assignment record; here just return ok
        return ResponseEntity
                .ok(Map.of("message", "Supervisor assigned (request created)", "supervisorId", sup.getUserId()));
    }

    @PutMapping("/supervisors/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        User u = userRepo.findById(id).orElseThrow();
        u.setStatus(status);
        userRepo.save(u);
        return ResponseEntity.ok(Map.of("message", "status updated"));
    }

    @GetMapping("/users/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        List<User> users = userRepo.findAll().stream()
                .filter(u -> role.equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
