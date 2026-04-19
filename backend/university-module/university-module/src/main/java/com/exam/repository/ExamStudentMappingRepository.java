package com.exam.repository;

import com.exam.entity.ExamStudentMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamStudentMappingRepository
        extends JpaRepository<ExamStudentMapping, Long> {
}
