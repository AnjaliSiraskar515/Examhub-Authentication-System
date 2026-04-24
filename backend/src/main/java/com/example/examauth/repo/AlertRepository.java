package com.example.examauth.repo;

import com.example.examauth.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByExamIdOrderByTimestampDesc(Long examId);

    List<Alert> findByExamIdAndIsReadFalse(Long examId);

    long countByIsReadFalse();

    List<Alert> findTop10ByOrderByTimestampDesc();
}
