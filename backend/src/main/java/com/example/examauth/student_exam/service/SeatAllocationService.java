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
import com.example.examauth.service.StudentExamEligibilityService;
import com.example.examauth.repo.ExamAttendanceRepository;
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

    @Transactional
    public void generateSeatAllocation(Long examId, Long collegeId) {
        // Safeguard: Prevent regeneration if verification or attendance has already started at THIS college
        if (examAttendanceRepository.existsByExamIdAndCollegeId(examId, collegeId)) {
            throw new IllegalStateException("Cannot regenerate seats: Attendance or verification has already started for this exam at this college.");
        }

        // 1. Idempotency: clear existing before regeneration
        seatAllocationRepository.deleteAllByExamIdAndCollegeId(examId, collegeId);

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

        // 4. Sort by PRN for deterministic roll number generation
        collegeRegistrations.sort(Comparator.comparing(r -> r.getPrn() != null ? r.getPrn() : ""));

        // 5. Generate allocations
        User anyStudent = userRepository.findById(collegeRegistrations.get(0).getStudentId()).orElseThrow();
        String collegeCode = anyStudent.getCollege() != null && anyStudent.getCollege().getCode() != null 
                ? anyStudent.getCollege().getCode() : "COL";
        String year = String.valueOf(java.time.Year.now().getValue());

        List<ExamSeatAllocation> allocations = new ArrayList<>();
        int studentIndex = 0;
        int serialInCollege = 1;

        for (ExamHall hall : halls) {
            int seatsInThisHall = 0;
            while (seatsInThisHall < hall.getCapacity() && studentIndex < collegeRegistrations.size()) {
                ExamRegistration reg = collegeRegistrations.get(studentIndex);
                User student = userRepository.findById(reg.getStudentId()).orElseThrow();

                ExamSeatAllocation alloc = new ExamSeatAllocation();
                alloc.setRegistrationId(reg.getId());
                alloc.setStudentId(reg.getStudentId());
                alloc.setPrn(reg.getPrn());
                alloc.setStudentName(student.getName());
                alloc.setExamId(examId);
                alloc.setCollegeId(collegeId);
                alloc.setCollegeName(student.getCollege().getName());
                alloc.setHall(hall);
                alloc.setHallName(hall.getHallName());

                // Standardized Format: PREFIX-XXX (e.g., A-001)
                alloc.setSeatNumber(String.format("%s-%03d", hall.getHallPrefix(), seatsInThisHall + 1));
                
                // Format: <collegeCode>/<year>/<3-digit serial> (resets per college as serialInCollege starts at 1)
                alloc.setRollNumber(String.format("%s/%s/%03d", collegeCode, year, serialInCollege));
                alloc.setSerialInCollege(serialInCollege);

                allocations.add(alloc);

                seatsInThisHall++;
                studentIndex++;
                serialInCollege++;
            }
            
            // Update seats assigned in hall
            hall.setSeatsAssigned(seatsInThisHall);
            examHallRepository.save(hall);
        }

        seatAllocationRepository.saveAll(allocations);

        // 6. Update Mapping Status
        ExamCollegeMapping mapping = mappingRepository.findByExamIdAndCollegeId(examId, collegeId).orElse(null);
        if (mapping != null) {
            mapping.setStatus(ExamCollegeMapping.MappingStatus.SEATS_GENERATED);
            mapping.setTotalStudents(collegeRegistrations.size());
            mappingRepository.save(mapping);
        }
    }
}
