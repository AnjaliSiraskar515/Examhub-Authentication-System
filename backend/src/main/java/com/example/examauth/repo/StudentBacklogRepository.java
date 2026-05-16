package com.example.examauth.repo;

import com.example.examauth.model.StudentBacklog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentBacklogRepository extends JpaRepository<StudentBacklog, Long> {
    List<StudentBacklog> findByStudentIdAndCleared(Long studentId, Boolean cleared);
    List<StudentBacklog> findByStudentId(Long studentId);
    Optional<StudentBacklog> findByStudentIdAndSubjectId(Long studentId, Long subjectId);
    List<StudentBacklog> findBySubjectId(Long subjectId);
    List<StudentBacklog> findBySubjectNameIgnoreCase(String subjectName);
    void deleteByStudentId(Long studentId);
}
