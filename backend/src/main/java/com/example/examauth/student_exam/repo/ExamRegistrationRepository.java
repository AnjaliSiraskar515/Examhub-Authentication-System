package com.example.examauth.student_exam.repo;

import com.example.examauth.student_exam.model.ExamRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRegistrationRepository extends JpaRepository<ExamRegistration, Long> {
    List<ExamRegistration> findByStudentId(Long studentId);

    boolean existsByPrnAndExamId(String prn, Long examId);

    // Primary duplicate check — uses authenticated studentId (avoids PRN collision
    // across users)
    boolean existsByStudentIdAndExamId(Long studentId, Long examId);

    long countByRegistrationStatus(com.example.examauth.student_exam.model.ExamRegistration.RegistrationStatus status);

    List<ExamRegistration> findByRegistrationStatus(
            com.example.examauth.student_exam.model.ExamRegistration.RegistrationStatus status);
}
