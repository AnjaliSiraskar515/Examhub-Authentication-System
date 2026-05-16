package com.example.examauth.student_exam.repo;

import com.example.examauth.student_exam.model.ExamHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamHallRepository extends JpaRepository<ExamHall, Long> {

    /** All halls configured for an exam at a specific college */
    List<ExamHall> findAllByExamIdAndCollegeId(Long examId, Long collegeId);

    /** All halls for a given exam (across all colleges) */
    List<ExamHall> findAllByExamId(Long examId);

    /** All halls created by a specific supervisor */
    List<ExamHall> findAllByCreatedBySupervisorId(Long supervisorId);

    /** Halls for a given exam at a college, created by a specific supervisor */
    List<ExamHall> findAllByExamIdAndCollegeIdAndCreatedBySupervisorId(
            Long examId, Long collegeId, Long supervisorId);

    /** Find hall by prefix within an exam+college (for uniqueness check) */
    Optional<ExamHall> findByExamIdAndCollegeIdAndHallPrefix(
            Long examId, Long collegeId, String hallPrefix);
            
    /** Find hall by name within an exam+college (for uniqueness check) */
    Optional<ExamHall> findByExamIdAndCollegeIdAndHallName(
            Long examId, Long collegeId, String hallName);

    /** Total capacity across all halls for a specific exam+college */
    @Query("SELECT COALESCE(SUM(h.capacity), 0) FROM ExamHall h WHERE h.examId = :examId AND h.collegeId = :collegeId")
    Integer sumCapacityByExamIdAndCollegeId(@Param("examId") Long examId, @Param("collegeId") Long collegeId);

    /** Count of halls configured for an exam+college */
    long countByExamIdAndCollegeId(Long examId, Long collegeId);

    /** Check if any hall exists for this exam+college */
    boolean existsByExamIdAndCollegeId(Long examId, Long collegeId);
}
