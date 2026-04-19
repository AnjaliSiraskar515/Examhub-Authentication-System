package com.exam.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.exam.entity.ExamAssignment;
import com.exam.entity.Supervisor;
import java.util.List;
import com.exam.entity.Exam;
import com.exam.entity.College;

public interface ExamAssignmentRepository 
        extends JpaRepository<ExamAssignment, Long> {

    List<ExamAssignment> findBySupervisor(Supervisor supervisor);

    boolean existsBySupervisorAndExamAndCollege(
    Supervisor supervisor,
    Exam exam,
    College college
);

    long countByStatus(String status);
}