package com.example.examauth.config;

import com.example.examauth.model.User;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.repo.ExamRepository;
import com.example.examauth.repo.InstitutionRepository;
import com.example.examauth.repo.SystemSettingRepository;
import com.example.examauth.model.Exam;
import com.example.examauth.model.SystemSetting;
import com.example.examauth.student_exam.university.model.UniversityExam;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import com.example.examauth.student_exam.model.ExamHall;
import com.example.examauth.student_exam.model.ExamSeatAllocation;
import com.example.examauth.student_exam.repo.ExamHallRepository;
import com.example.examauth.student_exam.repo.ExamSeatAllocationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataSeeder {

    /**
     * On startup: ensure session.timeout.minutes is at least 480 (8 hours) in the
     * DB.
     * This overrides any legacy 15-minute value that was previously stored.
     */
    @Bean(name = "fixSessionTimeout")
    public CommandLineRunner fixSessionTimeout(SystemSettingRepository systemSettingRepo) {
        return args -> {
            try {
                SystemSetting setting = systemSettingRepo.findByKey("session.timeout.minutes")
                        .orElseGet(SystemSetting::new);
                String current = setting.getValue();
                int currentVal = -1;
                try {
                    currentVal = Integer.parseInt(current != null ? current.trim() : "0");
                } catch (Exception ignored) {
                }
                if (currentVal < 60) {
                    setting.setKey("session.timeout.minutes");
                    setting.setValue("480");
                    setting.setUpdatedBy("system");
                    setting.setUpdatedAt(java.time.LocalDateTime.now());
                    systemSettingRepo.save(setting);
                    System.out.println("✅ Session timeout updated to 480 minutes (8 hours).");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Could not update session timeout: " + e.getMessage());
            }
        };
    }

    @Bean(name = "fixNullStatusUsers")
    public CommandLineRunner fixNullStatusUsers(UserRepository userRepository) {
        return args -> {
            List<User> nullStatusUsers = userRepository.findAll().stream()
                    .filter(u -> u.getStatus() == null || u.getStatus().trim().isEmpty())
                    .toList();
            if (!nullStatusUsers.isEmpty()) {
                nullStatusUsers.forEach(u -> u.setStatus("active"));
                userRepository.saveAll(nullStatusUsers);
                System.out.println(
                        "✅ Retroactively fixed " + nullStatusUsers.size() + " users with null status to 'active'.");
            }
        };
    }

    /**
     * Seeds a sample Hall + 10 Student Seat Allocations for the first COMPLETED
     * UniversityExam so the Download Report feature has real demo data.
     */
    @Bean(name = "seedCompletedExamData")
    public CommandLineRunner seedCompletedExamData(
            UniversityExamRepository universityExamRepo,
            ExamHallRepository examHallRepo,
            ExamSeatAllocationRepository seatAllocationRepo) {
        return args -> {
            try {
                // Find first completed exam
                UniversityExam completed = universityExamRepo.findAll().stream()
                        .filter(e -> "COMPLETED".equalsIgnoreCase(e.getStatus()))
                        .findFirst().orElse(null);
                if (completed == null)
                    return;

                Long examId = completed.getId();
                Long collegeId = completed.getCollegeId() != null ? completed.getCollegeId() : 1L;

                // Skip if data already exists
                long existingHalls = examHallRepo.findAllByExamIdAndCollegeId(examId, collegeId).size();
                if (existingHalls > 0)
                    return;

                // Create a demo hall
                ExamHall hall = new ExamHall();
                hall.setExamId(examId);
                hall.setCollegeId(collegeId);
                hall.setHallName("Hall A - Room 201");
                hall.setHallPrefix("A");
                hall.setCapacity(30);
                hall.setExamSupervisorName("AKS Supervisor");
                ExamHall savedHall = examHallRepo.save(hall);

                // Seed 10 sample seat allocations
                String[] names = { "Riya Sharma", "Amit Patil", "Sneha Joshi", "Rahul Desai",
                        "Pooja Kulkarni", "Vikas Rao", "Ananya Gupta", "Arjun Nair",
                        "Kavya Singh", "Omkar Bhat" };
                String[] prns = { "22260001", "22260002", "22260003", "22260004", "22260005",
                        "22260006", "22260007", "22260008", "22260009", "22260010" };

                for (int i = 0; i < names.length; i++) {
                    // Avoid duplicate seat allocations (use a fake registrationId per student)
                    long fakeRegId = examId * 1000 + (i + 1);
                    boolean exists = seatAllocationRepo.existsByRegistrationIdAndExamId(fakeRegId, examId);
                    if (exists)
                        continue;

                    ExamSeatAllocation seat = new ExamSeatAllocation();
                    seat.setRegistrationId(fakeRegId);
                    seat.setStudentId((long) (900 + i));
                    seat.setPrn(prns[i]);
                    seat.setStudentName(names[i]);
                    seat.setExamId(examId);
                    seat.setCollegeId(collegeId);
                    seat.setCollegeName(completed.getCollegeId() != null ? "Demo College" : "SRCOE");
                    seat.setHall(savedHall);
                    seat.setHallName(savedHall.getHallName());
                    seat.setSeatNumber("A-" + String.format("%03d", i + 1));
                    seat.setRollNumber("CLG/2026/" + String.format("%03d", i + 1));
                    seat.setSerialInCollege(i + 1);
                    seatAllocationRepo.save(seat);
                }
                System.out.println("✅ Seeded Hall + 10 seat allocations for completed exam ID: " + examId);
            } catch (Exception e) {
                System.err.println("⚠️ Could not seed completed exam data: " + e.getMessage());
            }
        };
    }

    @Bean(name = "backfillUniversityExamInstitution")
    public CommandLineRunner backfillUniversityExamInstitution(
            UniversityExamRepository universityExamRepo,
            UserRepository userRepo,
            InstitutionRepository institutionRepo) {
        return args -> {
            List<UniversityExam> nullCodeExams = universityExamRepo.findAll().stream()
                    .filter(e -> e.getInstitutionCode() == null || e.getInstitutionCode().isEmpty())
                    .toList();
            if (nullCodeExams.isEmpty())
                return;

            java.util.Set<Long> supervisorIds = nullCodeExams.stream()
                    .map(UniversityExam::getSupervisorId)
                    .filter(id -> id != null)
                    .collect(java.util.stream.Collectors.toSet());

            if (supervisorIds.isEmpty())
                return;

            // Build a map: supervisorId → institutionCode (via supervisor user →
            // institution lookup)
            java.util.Map<Long, String> supervisorToInstitutionCode = new java.util.HashMap<>();
            userRepo.findAllById(supervisorIds).forEach(u -> {
                com.example.examauth.model.Institution inst = null;
                if (u.getInstitutionCode() != null && !u.getInstitutionCode().isEmpty()) {
                    inst = institutionRepo.findFirstByInstitutionCode(u.getInstitutionCode()).orElse(null);
                }
                if (inst == null && u.getEmail() != null && !u.getEmail().trim().isEmpty()) {
                    inst = institutionRepo.findFirstByAdminEmail(u.getEmail()).orElse(null);
                }
                if (inst == null && u.getEmail() != null && !u.getEmail().trim().isEmpty()) {
                    inst = institutionRepo.findFirstByContactEmail(u.getEmail()).orElse(null);
                }
                if (inst != null && inst.getInstitutionCode() != null) {
                    supervisorToInstitutionCode.put(u.getUserId(), inst.getInstitutionCode());
                }
            });

            boolean anyUpdated = false;
            for (UniversityExam exam : nullCodeExams) {
                if (exam.getSupervisorId() != null) {
                    String code = supervisorToInstitutionCode.get(exam.getSupervisorId());
                    if (code != null) {
                        exam.setInstitutionCode(code);
                        universityExamRepo.save(exam);
                        anyUpdated = true;
                    }
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
            // userRepository.findFirstByEmail("anjali.siraskar05@gmail.com").ifPresent(u ->
            // {
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
            if (userRepository.findFirstByEmail("its972025@gmail.com").isEmpty()) {
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
            // DELETE FROM exams WHERE exam_name IN ('Advanced Java Programming','Theory of
            // Computation','Operating System');
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

            // Seed 30 Test Students for Phase 5 E2E Capacity & Pagination Testing
            for (int i = 1; i <= 30; i++) {
                String username = "e2e_student" + i;
                if (userRepository.findByUsername(username).isEmpty()) {
                    String prn = String.format("E2E%03d", i);
                    User s = new User();
                    s.setName("E2E Student " + i);
                    s.setEmail("e2e" + i + "@examhub.edu");
                    s.setUsername(username);
                    s.setPrn(prn);
                    s.setPassword(passwordEncoder.encode("password"));
                    s.setRole("STUDENT");
                    s.setStatus("APPROVED");
                    s.setProfileCompleted(true);
                    s.setDepartment("Computer Science");
                    s.setYear("Final Year");
                    s.setPhotoPath("https://ui-avatars.com/api/?name=E2E+" + i + "&background=random");
                    userRepository.save(s);
                }
            }

            System.out.println("✅ Test students seeded (including 30 E2E students)");

            // Seed Super Admin
            if (userRepository.findByUsername("super_admin").isEmpty()
                    && userRepository.findFirstByEmail("admin@examhub.com").isEmpty()) {
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

            // Ensure Supervisor A is EXAM type and Supervisor B is HEAD type on every
            // startup
            try {
                userRepository.findFirstByEmail("photosfor544@gmail.com").ifPresent(supA -> {
                    if (!"EXAM".equals(supA.getSupervisorType())) {
                        supA.setSupervisorType("EXAM");
                        userRepository.save(supA);
                        System.out.println("✅ Supervisor A set to EXAM type.");
                    }
                });
            } catch (Exception e) {
                System.out.println("Could not ensure Supervisor A type due to: " + e.getMessage());
            }

            try {
                userRepository.findFirstByEmail("examhub001@gmail.com").ifPresent(supB -> {
                    if (!"HEAD".equals(supB.getSupervisorType())) {
                        supB.setSupervisorType("HEAD");
                        userRepository.save(supB);
                        System.out.println("✅ Supervisor B ensured as HEAD type.");
                    }
                });
            } catch (Exception e) {
                System.out.println("Could not ensure Supervisor B type due to: " + e.getMessage());
            }

        };
    }
}
