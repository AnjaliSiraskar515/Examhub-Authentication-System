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

import java.util.UUID;

@Service
public class UserManagementService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

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
            Long collegeId) {
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

        userRepository.save(user);
    }

    @Transactional
    public void createSupervisor(String name, String email, String phone, String designation, String department,
            Long collegeId) {
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

        if (user.getPassword() == null) {
            String plainPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
            user.setPassword(passwordEncoder.encode(plainPassword));
            user.setFirstLogin(true);
            alertNotificationService.sendSupervisorCredentials(email, name, plainPassword);
        }

        userRepository.save(user);
    }
}
