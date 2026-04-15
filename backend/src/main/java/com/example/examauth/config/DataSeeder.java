package com.example.examauth.config;

import com.example.examauth.model.User;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.repo.ExamRepository;
import com.example.examauth.repo.InstitutionRepository;
import com.example.examauth.model.Exam;
import com.example.examauth.student_exam.university.model.UniversityExam;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean(name = "backfillUniversityExamInstitution")
    public CommandLineRunner backfillUniversityExamInstitution(
            UniversityExamRepository universityExamRepo,
            UserRepository userRepo,
            InstitutionRepository institutionRepo) {
        return args -> {
            List<UniversityExam> nullCodeExams = universityExamRepo.findAll().stream()
                    .filter(e -> e.getInstitutionCode() == null || e.getInstitutionCode().isEmpty())
                    .toList();
            if (nullCodeExams.isEmpty()) return;

            // Build a map: supervisorId → institutionCode (via supervisor user → institution lookup)
            java.util.Map<Long, String> supervisorToInstitutionCode = new java.util.HashMap<>();
            userRepo.findAll().stream()
                    .filter(u -> u.getUserId() != null)
                    .forEach(u -> {
                        com.example.examauth.model.Institution inst = null;
                        if (u.getInstitutionCode() != null && !u.getInstitutionCode().isEmpty()) {
                            inst = institutionRepo.findFirstByInstitutionCode(u.getInstitutionCode()).orElse(null);
                        }
                        if (inst == null) {
                            inst = institutionRepo.findFirstByAdminEmail(u.getEmail()).orElse(null);
                        }
                        if (inst == null) {
                            inst = institutionRepo.findFirstByContactEmail(u.getEmail()).orElse(null);
                        }
                        if (inst != null && inst.getInstitutionCode() != null) {
                            supervisorToInstitutionCode.put(u.getUserId(), inst.getInstitutionCode());
                        }
                    });

            boolean anyUpdated = false;
            for (UniversityExam exam : nullCodeExams) {
                String code = null;
                if (exam.getSupervisorId() != null) {
                    code = supervisorToInstitutionCode.get(exam.getSupervisorId());
                }
                if (code == null) {
                    // Fallback removed: we should not aggressively reassign exams to random institutions.
                }
                if (code != null) {
                    exam.setInstitutionCode(code);
                    universityExamRepo.save(exam);
                    anyUpdated = true;
                }
            }
            if (anyUpdated) {
                System.out.println("✅ Backfilled institutionCode on existing UniversityExam records.");
            }
        };
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ExamRepository examRepository) {
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
                student.setPrn("TEST001");
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
                System.out.println("✅ Test Student Seeded: teststudent / password (PRN: TEST001)");
            } else {
                System.out.println("ℹ️ Test Student already exists.");
            }

            // Seed real student: its972025@gmail.com / PRN: 72260829C
            if (userRepository.findByEmail("its972025@gmail.com").isEmpty()) {
                User realStudent = new User();
                realStudent.setName("Student");
                realStudent.setEmail("its972025@gmail.com");
                realStudent.setPrn("72260829C");
                realStudent.setPassword(passwordEncoder.encode("123456"));
                realStudent.setRole("STUDENT");
                realStudent.setStatus("active");
                realStudent.setProfileCompleted(false);
                realStudent.setDepartment("Computer Science");
                userRepository.save(realStudent);
                System.out.println("✅ Real Student Seeded: its972025@gmail.com / 72260829C / password: 123456");
            }

            // ── OLD DEMO EXAM SEEDS REMOVED ──────────────────────────────────────────
            // Advanced Java Programming, Theory of Computation, and Operating System were
            // test/demo exams only. The real exam module (university wizard) is now in use.
            // To delete old records from DB:
            //   DELETE FROM exams WHERE exam_name IN ('Advanced Java Programming','Theory of Computation','Operating System');
            // ─────────────────────────────────────────────────────────────────────────

            // Seed 5 Random Students (kept for student registration / testing flows)
            if (userRepository.count() < 10) {
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
                        s.setPhotoPath("https://ui-avatars.com/api/?name=Student+" + i + "&background=random");
                        userRepository.save(s);
                    }
                }
            }

            // Seed 3 OS Students (kept for testing)
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
            System.out.println("✅ Test students seeded");

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
                admin.setPrn("SUPERADMIN001"); // Setting dummy PRN to avoid null constraint

                userRepository.save(admin);
                System.out.println("✅ Super Admin Seeded: admin@examhub.com / admin123");
            }

            // Retroactively assign "toc" ("Theory of Computation") and "Java" ("Advanced Java Programming") to photosfor544@gmail.com and SPPU
            userRepository.findByEmail("photosfor544@gmail.com").ifPresent(supervisor -> {
                userRepository.findByEmail("starits04@gmail.com").ifPresent(university -> {
                    List<Exam> tocExams = examRepository.findByExamName("Theory of Computation");
                    List<Exam> javaExams = examRepository.findByExamName("Advanced Java Programming");
                    
                    for (Exam e : tocExams) {
                        e.setSupervisorId(supervisor.getUserId());
                        e.setSupervisorName(supervisor.getName());
                        e.setInstitutionName(university.getUniversityName() != null ? university.getUniversityName() : university.getCollegeName() != null ? university.getCollegeName() : "SPPU");
                        examRepository.save(e);
                    }
                    for (Exam e : javaExams) {
                        e.setSupervisorId(supervisor.getUserId());
                        e.setSupervisorName(supervisor.getName());
                        e.setInstitutionName(university.getUniversityName() != null ? university.getUniversityName() : university.getCollegeName() != null ? university.getCollegeName() : "SPPU");
                        examRepository.save(e);
                    }
                    System.out.println("✅ Retroactively assigned TOC and Java to " + supervisor.getEmail() + " under " + university.getEmail());
                });
            });

        };
    }
}
