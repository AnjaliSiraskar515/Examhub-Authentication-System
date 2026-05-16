package com.example.examauth.controller;

import com.example.examauth.service.CsvProcessingService;
import com.example.examauth.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

import com.example.examauth.repo.UserRepository;
import com.example.examauth.model.User;
import com.example.examauth.repo.StudentBacklogRepository;
import com.example.examauth.model.StudentBacklog;
import com.example.examauth.repo.SubjectRepository;
import com.example.examauth.model.Subject;
import java.util.stream.Collectors;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/admin/students")
public class AdminStudentController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.examauth.repo.CollegeRepository collegeRepository;

    @Autowired
    private CsvProcessingService csvProcessingService;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private StudentBacklogRepository backlogRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private com.example.examauth.service.AlertNotificationService alertNotificationService;

    @GetMapping
    public ResponseEntity<List<User>> getAllStudents(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String department) {
        
        String __email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User __admin = userRepository.findFirstByEmailAndRole(__email, org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")).orElse(null);
        String __myUniv = (__admin != null && "UNIVERSITY_ADMIN".equals(__admin.getRole())) ? __admin.getUniversityName() : null;

        List<User> students = userRepository.findByRoleWithCollege("STUDENT");

        if (__myUniv != null && !__myUniv.isEmpty()) {
            students = students.stream().filter(u -> 
                __myUniv.equalsIgnoreCase(u.getUniversityName()) || 
                (u.getCollege() != null && __myUniv.equalsIgnoreCase(u.getCollege().getUniversityName()))
            ).collect(java.util.stream.Collectors.toList());
        }

        // Optional filtering in-memory
        if (collegeId != null) {
            // Resolve the college name so we can also match legacy users (collegeName only,
            // no FK)
            final String resolvedCollegeName = collegeRepository.findById(collegeId)
                    .map(c -> c.getName()).orElse(null);
            students = students.stream().filter(u -> {
                // Primary: College entity FK
                if (u.getCollege() != null)
                    return u.getCollege().getId().equals(collegeId);
                // Fallback: legacy collegeName string match
                return resolvedCollegeName != null && resolvedCollegeName.equalsIgnoreCase(u.getCollegeName());
            }).collect(Collectors.toList());
        } else if (college != null && !college.trim().isEmpty() && !college.equals("All Colleges")) {
            students = students.stream().filter(u -> {
                if (u.getCollege() != null)
                    return college.equalsIgnoreCase(u.getCollege().getName());
                return college.equalsIgnoreCase(u.getCollegeName());
            }).collect(Collectors.toList());
        }
        if (year != null && !year.trim().isEmpty() && !year.equals("All Years")) {
            // Note: DB might store "First Year" or semantic matches in "semester".
            students = students.stream().filter(u -> {
                if (u.getSemester() != null && u.getSemester().equalsIgnoreCase(year))
                    return true;
                if (u.getYear() != null && u.getYear().equalsIgnoreCase(year))
                    return true;

                // Extract numeric value from semester string robustly (e.g., "Semester 8", "08"
                // -> 8)
                int semNum = -1;
                if (u.getSemester() != null) {
                    try {
                        String clean = u.getSemester().replaceAll("[^0-9]", "");
                        if (!clean.isEmpty()) {
                            semNum = Integer.parseInt(clean);
                        }
                    } catch (Exception e) {
                    }
                }

                // mapping logic e.g. First Year -> Sem 1 or 2
                if (year.equals("First Year") && (semNum == 1 || semNum == 2))
                    return true;
                if (year.equals("Second Year") && (semNum == 3 || semNum == 4))
                    return true;
                if (year.equals("Third Year") && (semNum == 5 || semNum == 6))
                    return true;
                if (year.equals("Fourth Year") && (semNum == 7 || semNum == 8))
                    return true;
                return false;
            }).collect(Collectors.toList());
        }
        if (department != null && !department.trim().isEmpty() && !department.equals("All Departments")) {
            students = students.stream()
                    .filter(u -> {
                        String userDept = u.getDepartmentEntity() != null ? u.getDepartmentEntity().getName()
                                : u.getDepartment();
                        if ("Computer Engineering".equalsIgnoreCase(userDept))
                            userDept = "Computer Science";

                        String userMajor = u.getMajor();
                        if ("Computer Engineering".equalsIgnoreCase(userMajor))
                            userMajor = "Computer Science";

                        return (userDept != null && userDept.equalsIgnoreCase(department)) ||
                                (userMajor != null && userMajor.equalsIgnoreCase(department));
                    })
                    .collect(Collectors.toList());
        }

        // Dynamically compute year from semester if missing
        for (User u : students) {
            if ("Computer Engineering".equalsIgnoreCase(u.getDepartment())) {
                u.setDepartment("Computer Science");
            }
            if (u.getDepartmentEntity() != null
                    && "Computer Engineering".equalsIgnoreCase(u.getDepartmentEntity().getName())) {
                u.getDepartmentEntity().setName("Computer Science");
            }

            if (u.getYear() == null || u.getYear().trim().isEmpty()) {
                try {
                    int sem = Integer.parseInt(u.getSemester());
                    if (sem <= 2)
                        u.setYear("First Year");
                    else if (sem <= 4)
                        u.setYear("Second Year");
                    else if (sem <= 6)
                        u.setYear("Third Year");
                    else
                        u.setYear("Fourth Year");
                } catch (Exception e) {
                    u.setYear("N/A");
                }
            }
        }

        return ResponseEntity.ok(students);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadStudentCsv(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "collegeId", required = false) Long collegeId) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a CSV file!");
        }

        try {
            List<String[]> rows = csvProcessingService.parseCsv(file);
            int processedCount = 0;
            for (String[] row : rows) {
                if (row.length < 6)
                    continue; // Basic validation
                String prn = row[0];
                String name = row[1];
                String email = row[2];
                String course = row[3];
                String department = row[4];
                String semester = row[5];
                String backlogsStr = row.length > 6 ? row[6] : "";

                try {
                    userManagementService.createStudent(prn, name, email, course, department, semester, backlogsStr, collegeId);
                    processedCount++;
                } catch (IllegalArgumentException e) {
                    // Skip or log users failing validation
                }
            }
            return ResponseEntity.ok("Processed " + processedCount + " student records successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process CSV file: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> toggleStudentStatus(@PathVariable Long id,
            @RequestBody java.util.Map<String, Boolean> body) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
        Boolean active = body.get("active");
        // default missing to true or use the value provided
        if (active == null)
            active = true;
        user.setIsEligible(active);
        user.setExamAccessAllowed(active);
        userRepository.save(user);
        return ResponseEntity.ok("Student status updated.");
    }

    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> removeStudent(@PathVariable Long id) {
        // Capture user details BEFORE deletion for the notification email
        User removedUser = userRepository.findById(id).orElse(null);

        // Resolve the acting admin's university name for the email
        String universityName = "the University Administration";
        try {
            String adminEmail = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();
            User admin = userRepository.findFirstByEmail(adminEmail).orElse(null);
            if (admin != null) {
                if (admin.getUniversityName() != null && !admin.getUniversityName().isEmpty()) {
                    universityName = admin.getUniversityName();
                } else if (admin.getCollege() != null && admin.getCollege().getUniversityName() != null) {
                    universityName = admin.getCollege().getUniversityName();
                }
            }
        } catch (Exception ignored) {}

        // Delete associated backlogs first to avoid orphaned records
        // and ensure the same student can be re-imported via CSV
        backlogRepository.deleteByStudentId(id);
        userRepository.deleteById(id);

        // Send access-removal email notification asynchronously AFTER successful deletion
        if (removedUser != null && removedUser.getEmail() != null) {
            alertNotificationService.sendAccessRemovedEmail(
                    removedUser.getEmail(),
                    removedUser.getName() != null ? removedUser.getName() : "User",
                    removedUser.getRole(),
                    universityName
            );
        }

        return ResponseEntity.ok("Student removed.");
    }

    @GetMapping("/{id}/backlogs")
    public ResponseEntity<?> getStudentBacklogs(@PathVariable Long id) {
        List<StudentBacklog> backlogs = backlogRepository.findByStudentId(id);
        if (backlogs.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<java.util.Map<String, Object>> backlogDetails = backlogs.stream().map(b -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", b.getId());
            
            // Resolve subject name
            String subjName = b.getSubjectName();
            if ((subjName == null || subjName.isEmpty()) && b.getSubjectId() != null) {
                Subject s = subjectRepository.findById(b.getSubjectId()).orElse(null);
                if (s != null) {
                    subjName = s.getName();
                }
            }
            map.put("subject", subjName != null ? subjName : "Unknown Subject");
            
            // Resolve semester
            String sem = b.getSemester();
            if ((sem == null || sem.isEmpty()) && b.getSubjectId() != null) {
                Subject s = subjectRepository.findById(b.getSubjectId()).orElse(null);
                if (s != null && s.getSemester() != null) {
                    sem = String.valueOf(s.getSemester());
                }
            }
            map.put("semester", sem != null ? sem : "N/A");
            
            map.put("status", b.getStatus());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(backlogDetails);
    }
}
