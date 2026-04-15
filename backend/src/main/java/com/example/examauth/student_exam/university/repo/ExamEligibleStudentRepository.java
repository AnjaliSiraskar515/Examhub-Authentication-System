package com.example.examauth.student_exam.university.repo;

import com.example.examauth.student_exam.university.model.ExamEligibleStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for ExamEligibleStudent entity
 */
@Repository
public interface ExamEligibleStudentRepository extends JpaRepository<ExamEligibleStudent, Long> {

    /**
     * Find eligible student by PRN number and exam session
     * Used to check for duplicate entries before insertion
     * 
     * @param prnNumber   Student's PRN number
     * @param examSession Exam session identifier
     * @return Optional containing the student if found
     */
    Optional<ExamEligibleStudent> findByPrnNumberAndExamSession(String prnNumber, String examSession);

    /**
     * Check if student exists for given PRN and exam session
     * 
     * @param prnNumber   Student's PRN number
     * @param examSession Exam session identifier
     * @return true if exists, false otherwise
     */
    boolean existsByPrnNumberAndExamSession(String prnNumber, String examSession);
}
