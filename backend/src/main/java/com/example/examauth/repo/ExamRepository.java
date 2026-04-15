package com.example.examauth.repo;

import com.example.examauth.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByInstitutionName(String institutionName);

    List<Exam> findByExamName(String examName);
}
