package com.example.examauth.student_exam.service;

import com.example.examauth.model.User;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.student_exam.model.ExamHall;
import com.example.examauth.student_exam.model.ExamRegistration;
import com.example.examauth.student_exam.model.ExamSeatAllocation;
import com.example.examauth.student_exam.repo.ExamHallRepository;
import com.example.examauth.student_exam.repo.ExamRegistrationRepository;
import com.example.examauth.student_exam.repo.ExamSeatAllocationRepository;
import com.example.examauth.student_exam.university.model.ExamCollegeMapping;
import com.example.examauth.student_exam.university.repo.ExamCollegeMappingRepository;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import com.example.examauth.student_exam.university.model.UniversityExam;
import com.example.examauth.student_exam.university.model.UniversityExamSubject;
import com.example.examauth.service.StudentExamEligibilityService;
import com.example.examauth.repo.ExamAttendanceRepository;
import com.example.examauth.repo.CollegeRepository;
import com.example.examauth.model.College;
import com.example.examauth.exception.EligibilityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatAllocationService {

    private final ExamSeatAllocationRepository seatAllocationRepository;
    private final ExamHallRepository examHallRepository;
    private final ExamRegistrationRepository examRegistrationRepository;
    private final UserRepository userRepository;
    private final ExamCollegeMappingRepository mappingRepository;
    private final StudentExamEligibilityService studentExamEligibilityService;
    private final ExamAttendanceRepository examAttendanceRepository;
    private final UniversityExamRepository universityExamRepository;
    private final CollegeRepository collegeRepository;

    @Transactional
    public void generateSeatAllocation(Long examId, Long collegeId) {
        // Safeguard: Prevent regeneration if verification or attendance has already started at THIS college
        if (examAttendanceRepository.existsByExamIdAndCollegeId(examId, collegeId)) {
            throw new IllegalStateException("Cannot regenerate seats: Attendance or verification has already started for this exam at this college.");
        }

        // 1. Idempotency: clear existing before regeneration
        seatAllocationRepository.deleteAllByExamIdAndCollegeId(examId, collegeId);
        seatAllocationRepository.flush(); // Force delete execution before any inserts to avoid constraint violations

        // Fetch Exam with subjects eagerly loaded to prevent LazyInitializationException
        UniversityExam exam = universityExamRepository.findByIdWithSubjects(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        // 2. Fetch Halls
        List<ExamHall> halls = examHallRepository.findAllByExamIdAndCollegeId(examId, collegeId);
        if (halls.isEmpty()) {
            throw new IllegalStateException("No halls configured for this college and exam");
        }
        
        // Ensure hallPrefix is valid
        for (ExamHall hall : halls) {
            if (hall.getHallPrefix() == null || hall.getHallPrefix().trim().isEmpty()) {
                throw new IllegalStateException("Invalid hall configuration: Hall prefix cannot be empty.");
            }
        }

        // Sort halls by prefix to maintain consistent distribution order
        halls.sort(Comparator.comparing(ExamHall::getHallPrefix));

        int totalCapacity = halls.stream().mapToInt(ExamHall::getCapacity).sum();

        // 3. Fetch Eligible Students
        // Using existing StudentExamEligibilityService for correct filtering (backlog, subject rules, fees, etc.)
        List<ExamRegistration> allRegistrations = examRegistrationRepository.findByExamId(examId);
        
        List<ExamRegistration> collegeRegistrations = new ArrayList<>();
        for (ExamRegistration reg : allRegistrations) {
            if (reg.getRegistrationStatus() == ExamRegistration.RegistrationStatus.APPROVED) {
                User student = userRepository.findById(reg.getStudentId()).orElse(null);
                if (student != null && student.getCollege() != null && student.getCollege().getId().equals(collegeId)) {
                    // Eligibility Gate via existing service
                    try {
                        // This applies the strict rules (fees, access, backlog vs regular status)
                        studentExamEligibilityService.getEligibleExams(student);
                        collegeRegistrations.add(reg);
                    } catch (EligibilityException e) {
                        log.warn("Student bypassed in seat allocation due to eligibility rules | StudentID: {}, PRN: {}, Reason: {}", 
                                student.getUserId(), student.getPrn(), e.getMessage());
                    }
                }
            }
        }

        if (collegeRegistrations.isEmpty()) {
            throw new IllegalStateException("No eligible students found to allocate seats for this exam at this college.");
        }

        if (totalCapacity < collegeRegistrations.size()) {
            throw new IllegalStateException("Not enough hall capacity. Capacity: " + totalCapacity + ", Students: " + collegeRegistrations.size());
        }

        // --- WIPE OLD DATA BEFORE PROCEEDING ---
        // Ensure absolutely no old seats or roll numbers remain for this college before we start generating new ones
        seatAllocationRepository.deleteAllByExamIdAndCollegeId(examId, collegeId);
        seatAllocationRepository.flush();

        // 4. Shuffle the registrations to ensure randomized seat and roll number assignments on regeneration
        java.util.Collections.shuffle(collegeRegistrations);
        
        // Map to ensure roll numbers are permanently attached to the student regardless of shuffling
        java.util.Map<Long, Integer> regSerialMap = new java.util.HashMap<>();
        for (int i = 0; i < collegeRegistrations.size(); i++) {
            regSerialMap.put(collegeRegistrations.get(i).getId(), i + 1);
        }

        // 5. Generate allocations per subject
        // Use the code of the college where the exam is taking place
        College examCenter = collegeRepository.findById(collegeId).orElseThrow();
        String collegeCode = examCenter.getCode() != null ? examCenter.getCode() : "COL";
        String year = String.valueOf(java.time.Year.now().getValue());

        List<UniversityExamSubject> examSubjectsRaw = exam.getSubjects() != null ? exam.getSubjects() : List.of();
        // Deduplicate subjects (Hibernate EAGER lists can sometimes duplicate rows)
        List<UniversityExamSubject> examSubjects = new ArrayList<>();
        java.util.Set<Long> seenSubjIds = new java.util.HashSet<>();
        for (UniversityExamSubject s : examSubjectsRaw) {
            if (s.getId() != null && seenSubjIds.add(s.getId())) {
                examSubjects.add(s);
            }
        }

        List<ExamSeatAllocation> allocations = new ArrayList<>();
        
        // Deduplicate registrations just to be safe against data corruption
        java.util.Set<Long> seenRegIds = new java.util.HashSet<>();
        List<ExamRegistration> uniqueRegistrations = new ArrayList<>();
        for (ExamRegistration r : collegeRegistrations) {
            if (seenRegIds.add(r.getId())) {
                uniqueRegistrations.add(r);
            }
        }
        
        for (UniversityExamSubject subject : examSubjects) {
            // Filter registrations for this subject
            List<ExamRegistration> subjectRegistrations = new ArrayList<>();
            for (ExamRegistration reg : uniqueRegistrations) {
                List<String> selected = reg.getSelectedSubjects();
                if (selected == null || selected.isEmpty() || selected.stream().anyMatch(s -> s.equalsIgnoreCase(subject.getSubjectName()))) {
                    subjectRegistrations.add(reg);
                }
            }
            
            if (subjectRegistrations.isEmpty()) continue;
            
            // Shuffle the student list so they get a different seat arrangement for every subject!
            java.util.Collections.shuffle(subjectRegistrations);
            
            int studentIndex = 0;

            for (ExamHall hall : halls) {
                int seatsInThisHall = 0;
                while (seatsInThisHall < hall.getCapacity() && studentIndex < subjectRegistrations.size()) {
                    ExamRegistration reg = subjectRegistrations.get(studentIndex);
                    User student = userRepository.findById(reg.getStudentId()).orElseThrow();

                    ExamSeatAllocation alloc = new ExamSeatAllocation();
                    alloc.setRegistrationId(reg.getId());
                    alloc.setStudentId(reg.getStudentId());
                    alloc.setPrn(reg.getPrn());
                    alloc.setStudentName(student.getName());
                    alloc.setExamId(examId);
                    alloc.setSubjectId(subject.getId());
                    alloc.setCollegeId(collegeId);
                    alloc.setCollegeName(student.getCollege().getName());
                    alloc.setHall(hall);
                    alloc.setHallName(hall.getHallName());

                    // Standardized Format: PREFIX-XXX (e.g., A-001)
                    alloc.setSeatNumber(String.format("%s-%03d", hall.getHallPrefix(), seatsInThisHall + 1));
                    
                    // Format: <collegeCode>/<year>/<3-digit serial>
                    int serialInCollege = regSerialMap.get(reg.getId());
                    alloc.setRollNumber(String.format("%s/%s/%03d", collegeCode, year, serialInCollege));
                    alloc.setSerialInCollege(serialInCollege);

                    allocations.add(alloc);

                    seatsInThisHall++;
                    studentIndex++;
                }
                
                hall.setSeatsAssigned(seatsInThisHall);
                examHallRepository.save(hall);
            }
        }

        // 5. Final deduplication safety net before inserting
        java.util.Set<String> seenAlloc = new java.util.HashSet<>();
        List<ExamSeatAllocation> distinctAllocations = new ArrayList<>();
        for (ExamSeatAllocation a : allocations) {
            String key = a.getRegistrationId() + "-" + (a.getSubjectId() != null ? a.getSubjectId() : "null");
            if (seenAlloc.add(key)) {
                distinctAllocations.add(a);
            } else {
                System.out.println("WARNING: Prevented duplicate allocation for regId=" + a.getRegistrationId() + " subjectId=" + a.getSubjectId());
            }
        }

        seatAllocationRepository.saveAll(distinctAllocations);
        seatAllocationRepository.flush();

        // 6. Update Mapping Status
        ExamCollegeMapping mapping = mappingRepository.findByExamIdAndCollegeId(examId, collegeId).orElse(null);
        if (mapping != null) {
            mapping.setStatus(ExamCollegeMapping.MappingStatus.SEATS_GENERATED);
            mapping.setTotalStudents(collegeRegistrations.size());
            mappingRepository.save(mapping);
        }
    }
}
