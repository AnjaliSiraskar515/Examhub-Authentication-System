package com.example.examauth.student_exam.university.repo;

import com.example.examauth.student_exam.university.model.UniversityExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UniversityExamRepository extends JpaRepository<UniversityExam, Long> {

    List<UniversityExam> findByStatus(String status);

    List<UniversityExam> findBySessionName(String sessionName);

    List<UniversityExam> findBySupervisorId(Long supervisorId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"subjects"})
    @org.springframework.data.jpa.repository.Query("SELECT u FROM UniversityExam u WHERE u.id = :id")
    java.util.Optional<UniversityExam> findByIdWithSubjects(@org.springframework.data.repository.query.Param("id") Long id);
}
