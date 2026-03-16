package com.example.examauth.repo;

import com.example.examauth.model.ExamAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamAttendanceRepository extends JpaRepository<ExamAttendance, Long> {
    boolean existsByStudentIdAndExamId(Long studentId, Long examId);
    Optional<ExamAttendance> findByStudentIdAndExamId(Long studentId, Long examId);
}

