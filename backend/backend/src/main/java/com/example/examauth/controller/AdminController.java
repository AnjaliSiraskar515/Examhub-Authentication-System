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
        List<User> s = userRepo.findAll().stream().filter(u -> "supervisor".equalsIgnoreCase(u.getRole())).collect(Collectors.toList());
        return ResponseEntity.ok(s);
    }

    @PostMapping("/assign-supervisor")
    public ResponseEntity<?> assignSupervisor(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        Long examId = Long.valueOf(String.valueOf(body.get("examId")));
        // create or find supervisor user
        User sup = userRepo.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName(email.split("@")[0]);
            u.setRole("supervisor");
            u.setPassword("temp");
            return userRepo.save(u);
        });
        // In production create assignment record; here just return ok
        return ResponseEntity.ok(Map.of("message", "Supervisor assigned (request created)", "supervisorId", sup.getUserId()));
    }

    @PutMapping("/supervisors/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        User u = userRepo.findById(id).orElseThrow();
        u.setStatus(status);
        userRepo.save(u);
        return ResponseEntity.ok(Map.of("message", "status updated"));
    }
}
