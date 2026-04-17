package com.example.examauth.service;

import com.example.examauth.model.User;
import com.example.examauth.model.College;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.repo.CollegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserManagementService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollegeRepository collegeRepository;

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
    public void createStudent(String prn, String name, String email, String course, String department, String semester, Long collegeId) {
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
        user.setMajor(course); // Mapping course to major based on User.java properties
        user.setDepartment(department);
        user.setSemester(semester);
        
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
    public void createSupervisor(String name, String email, String phone, String designation, String department, Long collegeId) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Supervisor with email " + email + " already exists!");
        }

        if (collegeId == null) {
            throw new IllegalArgumentException("College ID is mandatory for new supervisors.");
        }

        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid College ID provided."));

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setDesignation(designation);
        user.setDepartment(department);
        
        user.setCollege(college);
        user.setCollegeName(college.getName());

        user.setRole("SUPERVISOR");
        user.setUsername(email);
        
        String plainPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(plainPassword));
        
        user.setFirstLogin(true);

        userRepository.save(user);
        
        alertNotificationService.sendSupervisorCredentials(email, name, plainPassword);
    }
}
