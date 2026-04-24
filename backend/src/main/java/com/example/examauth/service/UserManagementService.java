package com.example.examauth.service;

import com.example.examauth.model.User;
import com.example.examauth.model.College;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.repo.CollegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.examauth.model.Department;
import com.example.examauth.repo.DepartmentRepository;
import com.example.examauth.model.StudentBacklog;
import com.example.examauth.repo.StudentBacklogRepository;
import com.example.examauth.repo.SubjectRepository;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Service
public class UserManagementService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StudentBacklogRepository studentBacklogRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    private String normalizeDepartment(String dept) {
        if (dept == null || dept.trim().isEmpty()) {
            return "General";
        }
        dept = dept.trim().toLowerCase();
        String[] words = dept.split("\\s+");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        String result = capitalized.toString().trim();
        if (result.equalsIgnoreCase("Computer Engineering")) {
            return "Computer Science";
        }
        return result;
    }

    private College getOrCreateCollege(String collegeName) {
        String finalName = (collegeName != null && !collegeName.trim().isEmpty()) ? collegeName.trim() : "Unknown";
        return collegeRepository.findByNameIgnoreCase(finalName).orElseGet(() -> {
            College newCollege = new College();
            newCollege.setName(finalName);
            return collegeRepository.save(newCollege);
        });
    }

    @Autowired
    private AlertNotificationService alertNotificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void createStudent(String prn, String name, String email, String course, String department, String semester,
            String backlogs, Long collegeId) {
        if (userRepository.findByPrn(prn).isPresent()) {
            return; // Skip duplicate PRN
        }

        if (collegeId == null) {
            throw new IllegalArgumentException("College ID is mandatory for new students.");
        }

        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid College ID provided."));

        User user = new User();
        user.setPrn(prn);
        user.setName(name);
        user.setEmail(email);

        // Map remaining fields
        user.setCourse(course); // seamlessly updates major
        user.setSemester(semester);

        String normalizedDept = normalizeDepartment(department);
        Department deptEntity = departmentRepository.findByNameIgnoreCaseAndCollegeId(normalizedDept, collegeId)
                .orElseGet(() -> {
                    Department newDept = new Department();
                    newDept.setName(normalizedDept);
                    newDept.setCollege(college);
                    return departmentRepository.save(newDept);
                });

        user.setDepartmentEntity(deptEntity);
        user.setDepartment(deptEntity.getName());

        user.setCollege(college);
        user.setCollegeName(college.getName());

        user.setRole("STUDENT");
        user.setUsername(prn); // Use PRN as username

        String plainPassword = prn + "@123";
        user.setPassword(passwordEncoder.encode(plainPassword));

        user.setFirstLogin(true);
        user.setFeesPaid(true); // Initial state
        user.setIsEligible(true);
        user.setExamAccessAllowed(true);

        // Process Backlogs
        boolean hasBacklogs = false;
        List<StudentBacklog> backlogList = new ArrayList<>();
        java.util.Set<String> unique = new java.util.HashSet<>();
        
        if (backlogs != null && !backlogs.trim().isEmpty()) {
            String[] backlogItems = backlogs.split(";");
            for (String b : backlogItems) {
                b = b.trim();
                if (b.isEmpty()) continue;
                
                String[] parts = b.split("-");
                if (parts.length < 2) continue; // Prevent runtime crashes on invalid format
                
                hasBacklogs = true;
                String bCode = parts[0].trim();
                
                // Strip non-digits from semester to keep it consistently numeric (e.g. "Sem 4" -> "4")
                String bSemStr = parts[1].trim().replaceAll("[^0-9]", "");
                Integer bSem = null;
                try {
                    bSem = Integer.parseInt(bSemStr);
                } catch (NumberFormatException e) {
                    System.out.println("Warning: Invalid semester format in backlog: " + parts[1]);
                    continue;
                }
                
                String key = bCode + "-" + bSem;
                
                if (!unique.contains(key)) {
                    unique.add(key);
                    
                    // Fallback create Subject if needed
                    Integer finalBSem = bSem;
                    com.example.examauth.model.Subject subject = subjectRepository.findByCodeAndSemesterAndDepartmentEntityIdAndCourse(bCode, bSem, deptEntity.getId(), course).orElseGet(() -> {
                        System.out.println("Auto-creating Subject from Backlog mapping: Code=" + bCode + " Sem=" + finalBSem + " Dept=" + deptEntity.getName() + " Course=" + course);
                        com.example.examauth.model.Subject newSub = new com.example.examauth.model.Subject();
                        newSub.setCode(bCode);
                        newSub.setName(bCode); // Name defaults to code for fallback
                        newSub.setSemester(finalBSem);
                        newSub.setCourse(course);
                        newSub.setDepartmentEntity(deptEntity);
                        return subjectRepository.save(newSub);
                    });

                    StudentBacklog sb = new StudentBacklog();
                    sb.setSubjectName(subject.getName());
                    sb.setSubjectId(subject.getId());
                    sb.setSemester(String.valueOf(bSem));
                    sb.setCleared(false);
                    backlogList.add(sb);
                }
            }
        }

        user.setStudentType(hasBacklogs ? "BACKLOG" : "REGULAR");
        User savedUser = userRepository.save(user);

        // Save Backlogs with savedUser ID
        for (StudentBacklog sb : backlogList) {
            sb.setStudentId(savedUser.getUserId());
            studentBacklogRepository.save(sb);
        }
    }

    @Transactional
    public void createSupervisor(String name, String email, String phone, String designation, String department,
            Long collegeId, String supervisorType) {
        if (collegeId == null) {
            throw new IllegalArgumentException("College ID is mandatory for new supervisors.");
        }

        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid College ID provided."));

        User user = userRepository.findByEmail(email).orElse(new User());
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setDesignation(designation);
        user.setSupervisorType(supervisorType != null ? supervisorType.toUpperCase() : "EXAM");

        String normalizedDept = normalizeDepartment(department);
        Department deptEntity = departmentRepository.findByNameIgnoreCaseAndCollegeId(normalizedDept, collegeId)
                .orElseGet(() -> {
                    Department newDept = new Department();
                    newDept.setName(normalizedDept);
                    newDept.setCollege(college);
                    return departmentRepository.save(newDept);
                });

        user.setDepartmentEntity(deptEntity);
        user.setDepartment(deptEntity.getName());

        user.setCollege(college);
        user.setCollegeName(college.getName());

        user.setRole("SUPERVISOR");
        if (user.getUsername() == null) {
            user.setUsername(email);
        }

        if (user.getPrn() == null) {
            // Assign dummy PRN for supervisors to bypass NOT NULL DB constraint
            user.setPrn("SUP_" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        if (user.getPassword() == null) {
            String plainPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
            user.setPassword(passwordEncoder.encode(plainPassword));
            user.setFirstLogin(true);
            alertNotificationService.sendSupervisorCredentials(email, name, plainPassword);
        }

        userRepository.save(user);
    }
}
