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

    @Autowired
    private com.example.examauth.repo.ExamRepository examRepo;

    @Autowired
    private com.example.examauth.repo.InstitutionRepository institutionRepo;

    @Autowired
    private com.example.examauth.service.AdminService adminService;

    @Autowired
    private com.example.examauth.service.PdfService pdfService;

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }

    @GetMapping("/activity-log")
    public ResponseEntity<?> getActivityLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getActivityLog(page, size));
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary() {
        long users = userRepo.findAll().stream().filter(u -> "student".equalsIgnoreCase(u.getRole())).count();
        long exams = examRepo.count();
        long institutions = institutionRepo.count();
        long qrs = qrRepo.count();
        long frauds = fraudRepo.count();
        return ResponseEntity.ok(Map.of(
                "users", users,
                "exams", exams,
                "institutions", institutions,
                "qrs", qrs,
                "frauds", frauds));
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


    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> downloadReport() {
        try {
            // Re-use summary logic to get data
            long users = userRepo.findAll().stream().filter(u -> "student".equalsIgnoreCase(u.getRole())).count();
            long exams = examRepo.count();
            long institutions = institutionRepo.count();
            long qrs = qrRepo.count();
            long frauds = fraudRepo.count();
            
            Map<String, Object> metrics = Map.of(
                    "users", users,
                    "exams", exams,
                    "institutions", institutions,
                    "qrs", qrs,
                    "frauds", frauds
            );

            byte[] pdfBytes = pdfService.generateSystemReport(metrics);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ExamHub_Report.pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/security/pdf")
    public ResponseEntity<byte[]> downloadSecurityReport() {
        try {
            long frauds = fraudRepo.count();
            // Mock bio failures for demo
            byte[] pdfBytes = pdfService.generateSecurityReport(frauds, 12);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Security_Report.pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/rankings/pdf")
    public ResponseEntity<byte[]> downloadRankingsReport() {
        try {
            byte[] pdfBytes = pdfService.generateRankingsReport();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Institution_Rankings.pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/exams/pdf")
    public ResponseEntity<byte[]> downloadExamReport() {
        try {
            long exams = examRepo.count();
            byte[] pdfBytes = pdfService.generateExamReport(exams);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Exam_Performance_Report.pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/logs/csv")
    public ResponseEntity<byte[]> downloadActivityLog() {
        try {
            byte[] csvBytes = pdfService.generateActivityLogCsv();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=System_Activity_Log.csv")
                    .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                    .body(csvBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/performance/pdf")
    public ResponseEntity<byte[]> downloadPerformanceReport() {
        try {
            byte[] pdfBytes = pdfService.generatePerformanceTrendsReport();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Performance_Trends_Report.pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/trends/pdf")
    public ResponseEntity<byte[]> downloadTrendsReport() {
        try {
            byte[] pdfBytes = pdfService.generateDeepDiveReport();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Deep_Dive_Analytics_Report.pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/system/csv")
    public ResponseEntity<byte[]> downloadSystemCsv() {
        try {
            long users = userRepo.findAll().stream().filter(u -> "student".equalsIgnoreCase(u.getRole())).count();
            long exams = examRepo.count();
            long institutions = institutionRepo.count();
            long qrs = qrRepo.count();
            long frauds = fraudRepo.count();
            
            Map<String, Object> metrics = Map.of(
                    "users", users,
                    "exams", exams,
                    "institutions", institutions,
                    "qrs", qrs,
                    "frauds", frauds
            );

            byte[] csvBytes = pdfService.generateSystemMetricsCsv(metrics);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=System_Intelligence_Report.csv")
                    .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                    .body(csvBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
