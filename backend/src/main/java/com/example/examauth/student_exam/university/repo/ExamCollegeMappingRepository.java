package com.example.examauth.student_exam.university.repo;

import com.example.examauth.student_exam.university.model.ExamCollegeMapping;
import com.example.examauth.student_exam.university.model.ExamCollegeMapping.MappingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamCollegeMappingRepository extends JpaRepository<ExamCollegeMapping, Long> {

    /** All college mappings for a given exam (University Admin overview) */
    List<ExamCollegeMapping> findAllByExamId(Long examId);

    /** All exam mappings assigned to a given Head Supervisor */
    List<ExamCollegeMapping> findAllByHeadSupervisorId(Long headSupervisorId);

    /** All exams for a specific college */
    List<ExamCollegeMapping> findAllByCollegeId(Long collegeId);

    /** Specific mapping for an exam+college combination */
    Optional<ExamCollegeMapping> findByExamIdAndCollegeId(Long examId, Long collegeId);

    /** All mappings for a given Head Supervisor with a specific status */
    List<ExamCollegeMapping> findAllByHeadSupervisorIdAndStatus(Long headSupervisorId, MappingStatus status);

    /** Count how many colleges are in a given status for an exam */
    long countByExamIdAndStatus(Long examId, MappingStatus status);

    /** Check if a mapping already exists (idempotent creation) */
    boolean existsByExamIdAndCollegeId(Long examId, Long collegeId);

    /** All active (non-COMPLETED) mappings for a supervisor */
    @Query("SELECT m FROM ExamCollegeMapping m WHERE m.headSupervisorId = :supervisorId AND m.status != 'COMPLETED'")
    List<ExamCollegeMapping> findActiveMappingsBySupervisor(@Param("supervisorId") Long supervisorId);

    /** All mappings where seat generation is complete — for university admin reporting */
    @Query("SELECT m FROM ExamCollegeMapping m WHERE m.examId = :examId AND m.status = 'SEATS_GENERATED'")
    List<ExamCollegeMapping> findSeatGeneratedMappingsByExam(@Param("examId") Long examId);
}
