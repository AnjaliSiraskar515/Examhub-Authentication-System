package com.example.examauth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.model.User;
import com.example.examauth.repo.QRCodeRepository;
import com.example.examauth.model.QRCodeEntry;
import com.example.examauth.repo.FraudLogRepository;
import com.example.examauth.model.FraudLog;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

    @Autowired
    private com.example.examauth.repo.CollegeRepository collegeRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${file.upload-dir:uploads/profile}")
    private String baseUploadDir;

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
    public ResponseEntity<?> getSupervisors(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) String department) {
        // Resolve college name for legacy fallback matching
        final String resolvedCollegeName = (collegeId != null)
                ? collegeRepo.findById(collegeId).map(c -> c.getName()).orElse(null)
                : null;

        List<Map<String, Object>> s = userRepo.findAll().stream()
                .filter(u -> "SUPERVISOR".equalsIgnoreCase(u.getRole()))
                .filter(u -> {
                    // If collegeId filter provided, restrict to that college only
                    if (collegeId != null) {
                        // Primary: College entity FK
                        if (u.getCollege() != null && !collegeId.equals(u.getCollege().getId())) return false;
                        // Fallback: legacy collegeName string match
                        if (u.getCollege() == null && (resolvedCollegeName == null || !resolvedCollegeName.equalsIgnoreCase(u.getCollegeName()))) return false;
                    }
                    // If department filter provided
                    if (department != null && !department.trim().isEmpty() && !department.equals("All Departments")) {
                        if (u.getDepartment() == null || !u.getDepartment().equalsIgnoreCase(department)) return false;
                    }
                    return true;
                })
                .map(u -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("userId", u.getUserId());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("status", u.getStatus());
                    map.put("role", u.getRole());
                    map.put("department", u.getDepartment()); // Expose department
                    map.put("designation", u.getDesignation());
                    map.put("collegeName", (u.getCollege() != null && u.getCollege().getName() != null) ? u.getCollege().getName() : u.getCollegeName());
                    map.put("collegeId", u.getCollege() != null ? u.getCollege().getId() : null);
                    map.put("photoPath", u.getPhotoPath()); // Profile photo
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

    @PatchMapping("/supervisors/{id}/department")
    public ResponseEntity<?> updateSupervisorDepartment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String department = body.get("department");
        User u = userRepo.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        u.setDepartment(department);
        userRepo.save(u);
        return ResponseEntity.ok(Map.of("message", "Department updated", "department", department));
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

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
            return ResponseEntity.status(401).body(Map.<String, Object>of("error", "Unauthorized"));
        }
        String email = authentication.getName();
        User user = userRepo.findFirstByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.<String, Object>of("error", "User not found"));
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getUserId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("role", user.getRole());
        profile.put("status", user.getStatus());
        profile.put("lastLogin", user.getLastLogin());
        profile.put("photoPath", user.getPhotoPath());
        profile.put("profileCompleted", user.getProfileCompleted());
        // University identity fields
        String uniName = user.getUniversityName();
        String instCode = user.getInstitutionCode();

        if ((uniName == null || uniName.trim().isEmpty()) && "UNIVERSITY_ADMIN".equalsIgnoreCase(user.getRole())) {
            // 1. Primary Fallback: Use unique institutionCode if available
            if (instCode != null && !instCode.isEmpty()) {
                institutionRepo.findFirstByInstitutionCode(instCode).ifPresent(inst -> {
                    user.setUniversityName(inst.getName());
                    userRepo.save(user);
                });
            }
            
            // 2. Secondary Fallback: Check institutions table by email (existing logic)
            if (user.getUniversityName() == null || user.getUniversityName().isEmpty()) {
                institutionRepo.findFirstByContactEmail(user.getEmail()).ifPresent(inst -> {
                    user.setUniversityName(inst.getName());
                    user.setInstitutionCode(inst.getInstitutionCode());
                    userRepo.save(user);
                });
            }
            if (user.getUniversityName() == null || user.getUniversityName().isEmpty()) {
                institutionRepo.findFirstByAdminEmail(user.getEmail()).ifPresent(inst -> {
                    user.setUniversityName(inst.getName());
                    user.setInstitutionCode(inst.getInstitutionCode());
                    userRepo.save(user);
                });
            }
            uniName = user.getUniversityName();
            instCode = user.getInstitutionCode();
        }

        profile.put("universityName", uniName);
        profile.put("collegeName", user.getCollegeName());
        profile.put("institutionCode", instCode);
        profile.put("universityLogoPath", user.getUniversityLogoPath());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody Map<String, Object> body) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String email = authentication.getName();
        User user = userRepo.findFirstByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String newName = (String) body.get("name");
        String newEmail = (String) body.get("email");
        String newPhone = (String) body.get("phoneNumber");
        String newUniversityName = (String) body.get("universityName");

        if (newName != null && !newName.isEmpty()) {
            user.setName(newName);
        }
        if (newPhone != null && !newPhone.isEmpty()) {
            user.setPhoneNumber(newPhone);
        }
        if (newEmail != null && !newEmail.isEmpty() && !newEmail.equals(user.getEmail())) {
            if (userRepo.findFirstByEmail(newEmail).isPresent()) {
                return ResponseEntity.status(400).body(Map.of("error", "Email already in use"));
            }
            user.setEmail(newEmail);
        }
        if (newUniversityName != null && !newUniversityName.trim().isEmpty()) {
            user.setUniversityName(newUniversityName.trim());
        }

        userRepo.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    // ─── University Logo Upload ────────────────────────────────────────────────
    @PostMapping("/upload-logo")
    public ResponseEntity<?> uploadUniversityLogo(
            Authentication authentication,
            @RequestParam("logo") MultipartFile logo) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        if (logo == null || logo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }
        String contentType = logo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
        }
        try {
            User user = userRepo.findFirstByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Resolve uploads/logo directory relative to project working dir
            File logoDir = new File("uploads/logo");
            if (!logoDir.isAbsolute()) {
                logoDir = new File(System.getProperty("user.dir"), "uploads/logo");
            }
            if (!logoDir.exists()) {
                logoDir.mkdirs();
            }

            String filename = System.currentTimeMillis() + "_" + logo.getOriginalFilename();
            logo.transferTo(new File(logoDir, filename));

            user.setUniversityLogoPath(filename);
            userRepo.save(user);

            return ResponseEntity.ok(Map.of(
                "message", "Logo uploaded successfully",
                "logoPath", filename,
                "logoUrl",  "/uploads/logo/" + filename
            ));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(Authentication authentication, @RequestBody Map<String, String> body) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String email = authentication.getName();
        User user = userRepo.findFirstByEmail(email).orElse(null);
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
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
