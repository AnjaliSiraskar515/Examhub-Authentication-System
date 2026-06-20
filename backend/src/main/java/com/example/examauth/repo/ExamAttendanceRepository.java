package com.example.examauth.repo;

import com.example.examauth.model.ExamAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamAttendanceRepository extends JpaRepository<ExamAttendance, Long> {
    boolean existsByStudentIdAndExamId(Long studentId, Long examId);
    Optional<ExamAttendance> findFirstByStudentIdAndExamId(Long studentId, Long examId);
    Optional<ExamAttendance> findFirstByStudentIdAndExamIdAndSubjectId(Long studentId, Long examId, Long subjectId);
    boolean existsByExamId(Long examId);
    
    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(ea) > 0 THEN true ELSE false END FROM ExamAttendance ea JOIN User u ON ea.studentId = u.userId WHERE ea.examId = :examId AND u.college.id = :collegeId")
    boolean existsByExamIdAndCollegeId(@org.springframework.data.repository.query.Param("examId") Long examId, @org.springframework.data.repository.query.Param("collegeId") Long collegeId);
}

