package com.example.examauth.config;

import com.example.examauth.model.Exam;
import com.example.examauth.model.User;
import com.example.examauth.repo.ExamRepository;
import com.example.examauth.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, ExamRepository examRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed Student
            if (userRepository.findByUsername("teststudent").isEmpty()) {
                User student = new User();
                student.setName("Test Student");
                student.setEmail("test@student.com");
                student.setUsername("teststudent");
                student.setPassword(passwordEncoder.encode("password"));
                student.setRole("STUDENT");
                student.setStatus("APPROVED");
                student.setProfileCompleted(true);
                student.setPhoneNumber("9999999999");
                student.setAadharPath("/documents/aadhar_placeholder.pdf");
                student.setPhotoPath("https://ui-avatars.com/api/?name=Test+Student&background=random");
                student.setMarks10Path("/documents/marks10_placeholder.pdf");
                student.setMarks12Path("/documents/marks12_placeholder.pdf");
                student.setDepartment("Computer Science");
                student.setYear("Final Year");

                userRepository.save(student);
                System.out.println("✅ Test Student Seeded: teststudent / password");
            } else {
                System.out.println("ℹ️ Test Student already exists.");
            }

            // Seed Exam
            if (examRepository.count() == 0) {
                Exam exam = new Exam();
                exam.setExamName("Advanced Java Programming");
                exam.setInstitutionName("ExamHub University");
                exam.setDate(LocalDate.now());
                exam.setStartTime(LocalTime.of(10, 0));
                exam.setDurationMinutes(180);
                exam.setMode("OFFLINE");
                exam.setLocation("Hall A");
                exam.setStatus("UPCOMING");

                examRepository.save(exam);
                System.out.println("✅ Test Exam Seeded: Advanced Java Programming");
            } else {
                System.out.println("ℹ️ Exams already exist.");
            }
        };
    }
}
