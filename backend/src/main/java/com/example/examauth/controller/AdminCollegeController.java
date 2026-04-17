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

    @GetMapping
    public ResponseEntity<List<College>> getAllColleges() {
        return ResponseEntity.ok(collegeRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createCollege(@RequestBody Map<String, String> body) {
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
        college.setUniversityName(body.get("universityName"));

        return ResponseEntity.ok(collegeRepository.save(college));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCollegeCsv(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a CSV file!");
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
                    collegeRepository.save(c);
                    processedCount++;
                }
            }
            return ResponseEntity.ok(Map.of("message", "Successfully processed " + processedCount + " colleges."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to process CSV file.");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCollege(@PathVariable Long id, @RequestBody Map<String, String> body) {
        College college = collegeRepository.findById(id).orElse(null);
        if (college == null)
            return ResponseEntity.notFound().build();

        String name = body.get("name");
        if (name != null && !name.trim().isEmpty()) {
            college.setName(name.trim());
        }
        if (body.containsKey("code"))
            college.setCode(body.get("code"));
        if (body.containsKey("universityName"))
            college.setUniversityName(body.get("universityName"));

        return ResponseEntity.ok(collegeRepository.save(college));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCollege(@PathVariable Long id) {
        if (!collegeRepository.existsById(id))
            return ResponseEntity.notFound().build();
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
