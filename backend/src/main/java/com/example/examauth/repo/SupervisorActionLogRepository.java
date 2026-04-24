package com.example.examauth.repo;

import com.example.examauth.model.SupervisorActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupervisorActionLogRepository extends JpaRepository<SupervisorActionLog, Long> {
    List<SupervisorActionLog> findByExamId(Long examId);

    List<SupervisorActionLog> findBySupervisorId(Long supervisorId);

    List<SupervisorActionLog> findTop100ByOrderByTimestampDesc();
}
