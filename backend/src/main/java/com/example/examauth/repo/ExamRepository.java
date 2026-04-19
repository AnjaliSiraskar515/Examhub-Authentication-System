package com.example.examauth.repo;

import com.example.examauth.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.examauth.model.ExamType;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByInstitutionName(String institutionName);

    List<Exam> findByExamName(String examName);

    @Query("SELECT e FROM Exam e WHERE e.type = :type AND e.semester = :semester AND e.status = :status")
    List<Exam> findRegularExams(@Param("semester") String semester, @Param("type") ExamType type, @Param("status") String status);

    @Query("SELECT e FROM Exam e WHERE e.type = :type AND e.subjectId IN :subjectIds AND e.status = :status")
    List<Exam> findBacklogExams(@Param("subjectIds") List<Long> subjectIds, @Param("type") ExamType type, @Param("status") String status);
}
