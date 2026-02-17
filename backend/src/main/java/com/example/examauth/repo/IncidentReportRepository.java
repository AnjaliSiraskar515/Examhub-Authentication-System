package com.example.examauth.repo;

import com.example.examauth.model.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
    List<IncidentReport> findByExamId(Long examId);

    List<IncidentReport> findByStudentId(Long studentId);

    long countByExamId(Long examId);
}
