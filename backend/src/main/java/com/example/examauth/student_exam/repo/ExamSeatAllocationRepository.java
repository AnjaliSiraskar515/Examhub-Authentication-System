package com.example.examauth.student_exam.repo;

import com.example.examauth.student_exam.model.ExamSeatAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSeatAllocationRepository extends JpaRepository<ExamSeatAllocation, Long> {

    // ─────────────────────────────────────────────────────────────────────────
    // Primary lookups for admit card generation
    // ─────────────────────────────────────────────────────────────────────────

    /** Main lookup: find a student's seat for a specific registration and subject */
    Optional<ExamSeatAllocation> findByRegistrationIdAndSubjectId(Long registrationId, Long subjectId);

    /** Fallback or original method if subjectId is not provided */
    Optional<ExamSeatAllocation> findFirstByRegistrationId(Long registrationId);

    /** Find by student + exam (alternative admit card lookup) */
    Optional<ExamSeatAllocation> findByStudentIdAndExamId(Long studentId, Long examId);

    // ─────────────────────────────────────────────────────────────────────────
    // Seating chart queries (supervisor view)
    // ─────────────────────────────────────────────────────────────────────────

    /** All allocations for exam+college ordered by seat number for seating chart */
    List<ExamSeatAllocation> findAllByExamIdAndCollegeIdOrderBySeatNumber(Long examId, Long collegeId);

    /** Paginated allocations for exam+college ordered by seat number for large datasets */
    @Query("SELECT e FROM ExamSeatAllocation e WHERE e.id IN " +
           "(SELECT MIN(e2.id) FROM ExamSeatAllocation e2 WHERE e2.examId = :examId AND e2.collegeId = :collegeId GROUP BY e2.registrationId) " +
           "ORDER BY e.seatNumber")
    org.springframework.data.domain.Page<ExamSeatAllocation> findAllDistinctByRegistrationIdForExamAndCollege(
            @Param("examId") Long examId, 
            @Param("collegeId") Long collegeId, 
            org.springframework.data.domain.Pageable pageable);

    /** All allocations for exam+college ordered by roll number */
    List<ExamSeatAllocation> findAllByExamIdAndCollegeIdOrderByRollNumber(Long examId, Long collegeId);

    /** All allocations for a specific hall — via @ManyToOne relationship */
    @Query("SELECT a FROM ExamSeatAllocation a WHERE a.hall.id = :hallId ORDER BY a.seatNumber")
    List<ExamSeatAllocation> findAllByHallIdOrderBySeatNumber(@Param("hallId") Long hallId);

    // ─────────────────────────────────────────────────────────────────────────
    // University Admin overview queries
    // ─────────────────────────────────────────────────────────────────────────

    /** All allocations for a given exam (university admin full view) */
    List<ExamSeatAllocation> findAllByExamId(Long examId);

    // ─────────────────────────────────────────────────────────────────────────
    // Validation and safety checks
    // ─────────────────────────────────────────────────────────────────────────

    /** Check if seat allocations already exist — prevents duplicate generation */
    boolean existsByExamIdAndCollegeId(Long examId, Long collegeId);

    /** Check if a seat allocation exists for a specific registration+exam — used by seeder */
    boolean existsByRegistrationIdAndExamId(Long registrationId, Long examId);

    /** Count of allocated seats for capacity reporting */
    long countByExamIdAndCollegeId(Long examId, Long collegeId);

    /** Find by roll number within exam+college for supervisor quick lookup */
    Optional<ExamSeatAllocation> findByExamIdAndCollegeIdAndRollNumber(
            Long examId, Long collegeId, String rollNumber);

    /** Max serial in college — used to determine next serial during generation */
    @Query("SELECT COALESCE(MAX(a.serialInCollege), 0) FROM ExamSeatAllocation a " +
           "WHERE a.examId = :examId AND a.collegeId = :collegeId")
    Integer findMaxSerialByExamIdAndCollegeId(
            @Param("examId") Long examId,
            @Param("collegeId") Long collegeId);

    // ─────────────────────────────────────────────────────────────────────────
    // Re-generation support — delete existing before re-running
    // ─────────────────────────────────────────────────────────────────────────

    @Modifying
    @Transactional
    @Query("DELETE FROM ExamSeatAllocation e WHERE e.examId = :examId AND e.collegeId = :collegeId")
    void deleteAllByExamIdAndCollegeId(
            @Param("examId") Long examId,
            @Param("collegeId") Long collegeId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ExamSeatAllocation e WHERE e.registrationId IN :regIds")
    void deleteAllByRegistrationIds(@Param("regIds") List<Long> regIds);
}
