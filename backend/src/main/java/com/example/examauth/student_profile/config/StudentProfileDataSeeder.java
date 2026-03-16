package com.example.examauth.student_profile.config;

import com.example.examauth.student_profile.model.StudentProfile;
import com.example.examauth.student_profile.repo.StudentProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seeds default test data into the student_profiles table on application
 * startup.
 * Inserts only if the table is empty, so it is safe to run repeatedly.
 */
@Configuration
public class StudentProfileDataSeeder {

    @Bean(name = "studentProfileSeeder")
    public CommandLineRunner seedStudentProfiles(StudentProfileRepository studentProfileRepository) {
        return args -> {
            if (studentProfileRepository.count() == 0) {
                StudentProfile defaultProfile = new StudentProfile(
                        "PRN2024001",
                        "Arti Kulkarni",
                        "BE Computer Engineering",
                        "Final Year",
                        false, // verified
                        false // profileLocked
                );
                studentProfileRepository.save(defaultProfile);
                System.out.println("[StudentProfileDataSeeder] Default student profile seeded successfully.");
            } else {
                System.out.println("[StudentProfileDataSeeder] Table already has data. Skipping seed.");
            }
        };
    }
}
