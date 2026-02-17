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
            // Cleanup specific colliding email if it exists (Fix for user request)
            // COMMENTED OUT TO PREVENT DELETING THE ACTIVE USER
            // userRepository.findByEmail("anjali.siraskar05@gmail.com").ifPresent(u -> {
            // System.out.println("⚠️ Removing conflicting user:
            // anjali.siraskar05@gmail.com");
            // userRepository.delete(u);
            // });

            // Seed Dummy Student (ID 1) to ensure Test Student gets ID 2
            if (userRepository.findByUsername("dummy_student").isEmpty()) {
                User dummy = new User();
                dummy.setName("Dummy Student");
                dummy.setEmail("dummy@student.com");
                dummy.setUsername("dummy_student");
                dummy.setPassword(passwordEncoder.encode("password"));
                dummy.setRole("STUDENT");
                dummy.setStatus("APPROVED");
                userRepository.save(dummy);
            }

            // Seed Student (Will be ID 2)
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

            // Seed Second Exam (TOC)
            if (examRepository.findByExamName("Theory of Computation").isEmpty()) {
                Exam exam2 = new Exam();
                exam2.setExamName("Theory of Computation");
                exam2.setInstitutionName("ExamHub University");
                exam2.setDate(LocalDate.now());
                exam2.setStartTime(LocalTime.of(14, 0));
                exam2.setDurationMinutes(120);
                exam2.setMode("ONLINE");
                exam2.setLocation("Virtual Lab 1");
                exam2.setStatus("LIVE");
                examRepository.save(exam2);
                System.out.println("✅ Exam Seeded: Theory of Computation");
            }

            // Seed 5 Random Students
            if (userRepository.count() < 10) { // arbitrary check to avoid over-seeding
                for (int i = 1; i <= 5; i++) {
                    String username = "student" + i;
                    if (userRepository.findByUsername(username).isEmpty()) {
                        User s = new User();
                        s.setName("Student " + i);
                        s.setEmail("student" + i + "@examhub.edu");
                        s.setUsername(username);
                        s.setPassword(passwordEncoder.encode("password"));
                        s.setRole("STUDENT");
                        s.setStatus("APPROVED");
                        s.setProfileCompleted(true);
                        s.setDepartment("Computer Science");
                        s.setYear("Third Year");
                        // Random photos
                        s.setPhotoPath("https://ui-avatars.com/api/?name=Student+" + i + "&background=random");
                        userRepository.save(s);
                    }
                }
            }

            // Seed Operating System Exam (Live, Online)
            if (examRepository.findByExamName("Operating System").isEmpty()) {
                Exam exam3 = new Exam();
                exam3.setExamName("Operating System");
                exam3.setInstitutionName("ExamHub University");
                exam3.setDate(LocalDate.now());
                exam3.setStartTime(LocalTime.of(10, 0));
                exam3.setDurationMinutes(180);
                exam3.setMode("ONLINE");
                exam3.setLocation("Remote / Virtual");
                exam3.setStatus("LIVE");
                examRepository.save(exam3);
                System.out.println("✅ Exam Seeded: Operating System");
            }

            // Seed 3 OS Students
            for (int i = 1; i <= 3; i++) {
                String username = "os_student" + i;
                if (userRepository.findByUsername(username).isEmpty()) {
                    User s = new User();
                    s.setName("Student OS " + i);
                    s.setEmail("os" + i + "@examhub.edu");
                    s.setUsername(username);
                    s.setPassword(passwordEncoder.encode("password"));
                    s.setRole("STUDENT");
                    s.setStatus("APPROVED");
                    s.setProfileCompleted(true);
                    s.setDepartment("Computer Science");
                    s.setYear("Third Year");
                    s.setQrVerified(false);
                    s.setBiometricVerified(false);
                    s.setPhotoPath("https://ui-avatars.com/api/?name=Student+OS+" + i + "&background=random");
                    userRepository.save(s);
                }
            }
            System.out.println("✅ 5 Random Students Seeded");

            // Seed Super Admin
            if (userRepository.findByUsername("super_admin").isEmpty()
                    && userRepository.findByEmail("admin@examhub.com").isEmpty()) {
                User admin = new User();
                admin.setName("Super Admin");
                admin.setEmail("admin@examhub.com");
                admin.setUsername("super_admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("SUPER_ADMIN");
                admin.setStatus("ACTIVE");
                admin.setPhoneNumber("+91 98765 43210");
                admin.setProfileCompleted(true);
                admin.setPassportPhotoPath("/documents/passport_placeholder.jpg");
                // Add dummy paths for mandatory docs
                admin.setAadharPath("/documents/aadhar_placeholder.pdf");
                admin.setMarks10Path("/documents/marks10.pdf");
                admin.setMarks12Path("/documents/marks12.pdf");
                admin.setBiometricPath("biometric_data.bin");

                userRepository.save(admin);
                System.out.println("✅ Super Admin Seeded: admin@examhub.com / admin123");
            }

        };
    }
}
