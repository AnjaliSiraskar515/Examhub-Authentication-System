package com.example.examauth.exam_registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("examRegistrationModuleRepository")
public interface ExamRegistrationRepository extends JpaRepository<ExamRegistration, Long> {
    boolean existsByStudentIdAndExamId(Long studentId, Long examId);

    List<ExamRegistration> findByStudentId(Long studentId);
}
