package com.example.examauth.student_exam.repo;

import com.example.examauth.student_exam.model.ExamRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRegistrationRepository extends JpaRepository<ExamRegistration, Long> {
    List<ExamRegistration> findByStudentId(Long studentId);

    List<ExamRegistration> findByExamId(Long examId);

    boolean existsByPrnAndExamId(String prn, Long examId);

    List<ExamRegistration> findByExamIdAndRegistrationStatus(Long examId, ExamRegistration.RegistrationStatus status);

    // Primary duplicate check — uses authenticated studentId (avoids PRN collision
    // across users)
    boolean existsByStudentIdAndExamId(Long studentId, Long examId);

    long countByRegistrationStatus(com.example.examauth.student_exam.model.ExamRegistration.RegistrationStatus status);

    long countByExamId(Long examId);

    List<ExamRegistration> findByRegistrationStatus(
            com.example.examauth.student_exam.model.ExamRegistration.RegistrationStatus status);
}
