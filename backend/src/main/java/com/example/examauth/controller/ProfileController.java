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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${ai.verification.url:http://localhost:5001}")
    private String aiVerificationUrl;

    @GetMapping("/info")
    public ResponseEntity<?> getProfileInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String jwt = token.substring(7);
        String email = jwtUtil.extractUsername(jwt);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getUserId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("role", user.getRole());
        profile.put("prn", user.getPrn()); // PRN — unique per student, read-only

        // Mapped Fields for Frontend
        profile.put("rollNumber", user.getUsername());
        profile.put("course", user.getMajor());
        profile.put("branch", user.getDepartment());
        profile.put("year", user.getYear());
        profile.put("semester", user.getSemester());
        profile.put("enrollmentNo", user.getEnrollmentNo());
        profile.put("cgpa", user.getCgpa());
        profile.put("dob", user.getDob());
        profile.put("gender", user.getGender());
        profile.put("regNumber", "REG" + user.getUserId());
        profile.put("academicStatus", "Regular");
        profile.put("university", "Pune University (SPPU)");

        profile.put("photoPath", user.getPhotoPath());
        profile.put("passportPhotoPath", user.getPassportPhotoPath());
        profile.put("profileCompleted", user.getProfileCompleted());
        Map<String, Boolean> docs = new HashMap<>();
        docs.put("aadhar", user.getAadharPath() != null);
        docs.put("marks10", user.getMarks10Path() != null);
        docs.put("marks12", user.getMarks12Path() != null);
        docs.put("sem1Marksheet", user.getSem1MarksheetPath() != null);
        docs.put("sem2Marksheet", user.getSem2MarksheetPath() != null);
        docs.put("sem3Marksheet", user.getSem3MarksheetPath() != null);
        docs.put("sem4Marksheet", user.getSem4MarksheetPath() != null);
        docs.put("sem5Marksheet", user.getSem5MarksheetPath() != null);
        docs.put("sem6Marksheet", user.getSem6MarksheetPath() != null);
        docs.put("sem7Marksheet", user.getSem7MarksheetPath() != null);
        docs.put("sem8Marksheet", user.getSem8MarksheetPath() != null);
        docs.put("passportPhoto", user.getPassportPhotoPath() != null);
        profile.put("documents", docs);

        return ResponseEntity.ok(profile);
    }

    @PostMapping("/update-info")
    public ResponseEntity<?> updateProfileInfo(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> body) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }
            String jwt = token.substring(7);
            String email = jwtUtil.extractUsername(jwt);
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Editable fields
            if (body.containsKey("name") && !body.get("name").isBlank())
                user.setName(body.get("name"));
            if (body.containsKey("phoneNumber"))
                user.setPhoneNumber(body.get("phoneNumber"));
            if (body.containsKey("dob"))
                user.setDob(body.get("dob"));
            if (body.containsKey("gender"))
                user.setGender(body.get("gender"));
            if (body.containsKey("department"))
                user.setDepartment(body.get("department"));
            if (body.containsKey("major"))
                user.setMajor(body.get("major"));
            if (body.containsKey("year"))
                user.setYear(body.get("year"));
            if (body.containsKey("semester"))
                user.setSemester(body.get("semester"));
            if (body.containsKey("enrollmentNo"))
                user.setEnrollmentNo(body.get("enrollmentNo"));
            if (body.containsKey("cgpa"))
                user.setCgpa(body.get("cgpa"));
            // PRN is intentionally NOT editable

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Profile information updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Update failed: " + e.getMessage()));
        }
    }

    @Autowired
    private com.example.examauth.util.JwtUtil jwtUtil;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocuments(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "aadhar", required = false) MultipartFile aadhar,
            @RequestParam(value = "marks10", required = false) MultipartFile marks10,
            @RequestParam(value = "marks12", required = false) MultipartFile marks12,
            @RequestParam(value = "sem1Marksheet", required = false) MultipartFile sem1Marksheet,
            @RequestParam(value = "sem2Marksheet", required = false) MultipartFile sem2Marksheet,
            @RequestParam(value = "sem3Marksheet", required = false) MultipartFile sem3Marksheet,
            @RequestParam(value = "sem4Marksheet", required = false) MultipartFile sem4Marksheet,
            @RequestParam(value = "sem5Marksheet", required = false) MultipartFile sem5Marksheet,
            @RequestParam(value = "sem6Marksheet", required = false) MultipartFile sem6Marksheet,
            @RequestParam(value = "sem7Marksheet", required = false) MultipartFile sem7Marksheet,
            @RequestParam(value = "sem8Marksheet", required = false) MultipartFile sem8Marksheet,
            @RequestParam(value = "passportPhoto", required = false) MultipartFile passportPhoto) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String jwt = token.substring(7);
            String email = jwtUtil.extractUsername(jwt);

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Fix for relative path issue: Ensure it's absolute
            File dir = new File(uploadDir);
            if (!dir.isAbsolute()) {
                dir = new File(System.getProperty("user.dir"), uploadDir);
            }

            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created && !dir.exists()) {
                    throw new IOException("Failed to create upload directory: " + dir.getAbsolutePath());
                }
            }

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
            if (sem1Marksheet != null) {
                String path = timestamp + "_" + sem1Marksheet.getOriginalFilename();
                sem1Marksheet.transferTo(new File(dir, path));
                user.setSem1MarksheetPath(path);
            }
            if (sem2Marksheet != null) {
                String path = timestamp + "_" + sem2Marksheet.getOriginalFilename();
                sem2Marksheet.transferTo(new File(dir, path));
                user.setSem2MarksheetPath(path);
            }
            if (sem3Marksheet != null) {
                String path = timestamp + "_" + sem3Marksheet.getOriginalFilename();
                sem3Marksheet.transferTo(new File(dir, path));
                user.setSem3MarksheetPath(path);
            }
            if (sem4Marksheet != null) {
                String path = timestamp + "_" + sem4Marksheet.getOriginalFilename();
                sem4Marksheet.transferTo(new File(dir, path));
                user.setSem4MarksheetPath(path);
            }
            if (sem5Marksheet != null) {
                String path = timestamp + "_" + sem5Marksheet.getOriginalFilename();
                sem5Marksheet.transferTo(new File(dir, path));
                user.setSem5MarksheetPath(path);
            }
            if (sem6Marksheet != null) {
                String path = timestamp + "_" + sem6Marksheet.getOriginalFilename();
                sem6Marksheet.transferTo(new File(dir, path));
                user.setSem6MarksheetPath(path);
            }
            if (sem7Marksheet != null) {
                String path = timestamp + "_" + sem7Marksheet.getOriginalFilename();
                sem7Marksheet.transferTo(new File(dir, path));
                user.setSem7MarksheetPath(path);
            }
            if (sem8Marksheet != null) {
                String path = timestamp + "_" + sem8Marksheet.getOriginalFilename();
                sem8Marksheet.transferTo(new File(dir, path));
                user.setSem8MarksheetPath(path);
            }
            if (passportPhoto != null) {
                String path = timestamp + "_" + passportPhoto.getOriginalFilename();
                passportPhoto.transferTo(new File(dir, path));
                user.setPassportPhotoPath(path);
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "uploaded"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "upload failed: " + e.getMessage()));
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<?> completeProfile(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String jwt = token.substring(7);
            String email = jwtUtil.extractUsername(jwt);

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Check mandatory documents
            if (user.getAadharPath() == null || user.getMarks10Path() == null || user.getMarks12Path() == null ||
                    user.getPhoneNumber() == null || user.getPassportPhotoPath() == null) {
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

    @DeleteMapping("/document/{docType}")
    public ResponseEntity<?> deleteDocument(@RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String docType) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }
            String jwt = token.substring(7);
            String email = jwtUtil.extractUsername(jwt);
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            switch (docType) {
                case "aadhar":
                    user.setAadharPath(null);
                    break;
                case "marks10":
                    user.setMarks10Path(null);
                    break;
                case "marks12":
                    user.setMarks12Path(null);
                    break;
                case "passportPhoto":
                    user.setPassportPhotoPath(null);
                    break;
                case "sem1Marksheet":
                    user.setSem1MarksheetPath(null);
                    break;
                case "sem2Marksheet":
                    user.setSem2MarksheetPath(null);
                    break;
                case "sem3Marksheet":
                    user.setSem3MarksheetPath(null);
                    break;
                case "sem4Marksheet":
                    user.setSem4MarksheetPath(null);
                    break;
                case "sem5Marksheet":
                    user.setSem5MarksheetPath(null);
                    break;
                case "sem6Marksheet":
                    user.setSem6MarksheetPath(null);
                    break;
                case "sem7Marksheet":
                    user.setSem7MarksheetPath(null);
                    break;
                case "sem8Marksheet":
                    user.setSem8MarksheetPath(null);
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid document type: " + docType));
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "delete failed: " + e.getMessage()));
        }
    }
}
