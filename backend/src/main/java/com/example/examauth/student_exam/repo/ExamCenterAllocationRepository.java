package com.example.examauth.student_exam.repo;

import com.example.examauth.student_exam.model.ExamCenterAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamCenterAllocationRepository extends JpaRepository<ExamCenterAllocation, Long> {
}
