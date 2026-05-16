package com.exam.repository;

import com.exam.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamScheduleRepository
        extends JpaRepository<ExamSchedule, Long> {
}
