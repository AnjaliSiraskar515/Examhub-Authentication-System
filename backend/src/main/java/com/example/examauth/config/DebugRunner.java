package com.example.examauth.config;

import com.example.examauth.repo.UserRepository;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class DebugRunner implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UniversityExamRepository universityExamRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("================ DEBUG DATA ================");
        
        System.out.println("--- UNIVERSITY EXAMS ---");
        universityExamRepository.findAll().forEach(e -> {
            System.out.println("Exam: " + e.getSessionName() + ", InstCode: '" + e.getInstitutionCode() + "', Status: " + e.getStatus());
        });

        System.out.println("--- UNIVERSITY ADMINS ---");
        userRepository.findAll().stream().filter(u -> "UNIVERSITY_ADMIN".equals(u.getRole())).forEach(u -> {
            System.out.println("Admin: " + u.getName() + ", UnivName: '" + u.getUniversityName() + "', InstCode: '" + u.getInstitutionCode() + "'");
        });

        System.out.println("============================================");
    }
}
