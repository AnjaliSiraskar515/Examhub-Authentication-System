package com.example.examauth.controller;

import com.example.examauth.model.College;
import com.example.examauth.repo.CollegeRepository;
import com.example.examauth.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/colleges")
public class AdminCollegeController {

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.examauth.service.CsvProcessingService csvProcessingService;

    // Helper: get the university name of the currently authenticated UNIVERSITY_ADMIN
    private String getCurrentAdminUniversityName() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        String role = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        com.example.examauth.model.User admin = userRepository.findFirstByEmailAndRole(email, role).orElse(null);
        if (admin != null && "UNIVERSITY_ADMIN".equals(admin.getRole())) {
            String name = admin.getUniversityName();
            return name != null ? name.trim() : null;
        }
        return null; // Not a UNIVERSITY_ADMIN
    }

    private boolean isUniversityAdmin() {
        String role = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return "UNIVERSITY_ADMIN".equals(role);
    }

    @GetMapping
    public ResponseEntity<List<College>> getAllColleges() {
        String myUniv = getCurrentAdminUniversityName();
        boolean isUnivAdmin = isUniversityAdmin();

        List<College> colleges = collegeRepository.findAll();
        if (isUnivAdmin) {
            if (myUniv == null || myUniv.isEmpty()) {
                return ResponseEntity.ok(new java.util.ArrayList<>()); // Fail-closed
            }
            colleges = colleges.stream()
                .filter(c -> myUniv.equalsIgnoreCase(c.getUniversityName() != null ? c.getUniversityName().trim() : null))
                .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(colleges);
    }

    @PostMapping
    public ResponseEntity<?> createCollege(@RequestBody Map<String, String> body) {
        String myUniv = getCurrentAdminUniversityName();
        boolean isUnivAdmin = isUniversityAdmin();

        if (isUnivAdmin && (myUniv == null || myUniv.isEmpty())) {
            return ResponseEntity.status(403).body(Map.of("error", "University Admin profile incomplete. Cannot create college."));
        }

        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "College name is required"));
        }
        name = name.trim();

        if (collegeRepository.findByNameIgnoreCase(name).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "College already exists"));
        }

        College college = new College();
        college.setName(name);
        college.setCode(body.get("code"));

        if (isUnivAdmin) {
            college.setUniversityName(myUniv);
        } else {
            college.setUniversityName(body.get("universityName"));
        }

        return ResponseEntity.ok(collegeRepository.save(college));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCollegeCsv(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        
        String myUniv = getCurrentAdminUniversityName();
        boolean isUnivAdmin = isUniversityAdmin();

        if (isUnivAdmin && (myUniv == null || myUniv.isEmpty())) {
            return ResponseEntity.status(403).body(Map.of("error", "University Admin profile incomplete. Cannot upload colleges."));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload a CSV file!"));
        }
        try {
            List<String[]> rows = csvProcessingService.parseCsv(file);
            int processedCount = 0;
            for (String[] row : rows) {
                if (row.length < 1)
                    continue;
                String name = row[0].trim();
                String code = row.length > 1 ? row[1].trim() : null;

                if (!name.isEmpty() && !collegeRepository.findByNameIgnoreCase(name).isPresent()) {
                    College c = new College();
                    c.setName(name);
                    c.setCode(code);
                    if (isUnivAdmin) {
                        c.setUniversityName(myUniv);
                    } else if (row.length > 2) {
                        c.setUniversityName(row[2].trim()); // SUPER_ADMIN can provide universityName in column 3
                    }
                    collegeRepository.save(c);
                    processedCount++;
                }
            }
            return ResponseEntity.ok(Map.of("message", "Successfully processed " + processedCount + " colleges."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process CSV file."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCollege(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String myUniv = getCurrentAdminUniversityName();
        boolean isUnivAdmin = isUniversityAdmin();

        College college = collegeRepository.findById(id).orElse(null);
        if (college == null)
            return ResponseEntity.notFound().build();

        if (isUnivAdmin) {
            if (myUniv == null || myUniv.isEmpty() || !myUniv.equalsIgnoreCase(college.getUniversityName() != null ? college.getUniversityName().trim() : null)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized to update this college"));
            }
        }

        String name = body.get("name");
        if (name != null && !name.trim().isEmpty()) {
            college.setName(name.trim());
        }
        if (body.containsKey("code"))
            college.setCode(body.get("code"));
            
        // Only allow SUPER_ADMIN to change university name
        if (!isUnivAdmin && body.containsKey("universityName")) {
            college.setUniversityName(body.get("universityName"));
        }

        return ResponseEntity.ok(collegeRepository.save(college));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCollege(@PathVariable Long id) {
        String myUniv = getCurrentAdminUniversityName();
        boolean isUnivAdmin = isUniversityAdmin();

        College college = collegeRepository.findById(id).orElse(null);
        if (college == null)
            return ResponseEntity.notFound().build();

        if (isUnivAdmin) {
            if (myUniv == null || myUniv.isEmpty() || !myUniv.equalsIgnoreCase(college.getUniversityName() != null ? college.getUniversityName().trim() : null)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized to delete this college"));
            }
        }

        collegeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "College deleted successfully"));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getCollegeStats(@PathVariable Long id) {
        College college = collegeRepository.findById(id).orElse(null);
        if (college == null)
            return ResponseEntity.notFound().build();
        final String collegeName = college.getName(); // for legacy fallback matching

        long totalStudents = userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()) && (
                // Primary: matched via College entity (new records)
                (u.getCollege() != null && id.equals(u.getCollege().getId()))
                        ||
                        // Fallback: matched via collegeName string (legacy records)
                        (u.getCollege() == null && collegeName != null
                                && collegeName.equalsIgnoreCase(u.getCollegeName()))))
                .count();
        long totalSupervisors = userRepository.findAll().stream()
                .filter(u -> "SUPERVISOR".equalsIgnoreCase(u.getRole())
                        && ((u.getCollege() != null && id.equals(u.getCollege().getId()))
                                ||
                                (u.getCollege() == null && collegeName != null
                                        && collegeName.equalsIgnoreCase(u.getCollegeName()))))
                .count();
        return ResponseEntity.ok(Map.of(
                "totalStudents", totalStudents,
                "totalSupervisors", totalSupervisors));
    }
}
