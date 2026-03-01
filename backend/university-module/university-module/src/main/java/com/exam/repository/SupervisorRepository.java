package com.exam.repository;

import com.exam.entity.Supervisor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;   

public interface SupervisorRepository 
        extends JpaRepository<Supervisor, Long> {

    List<Supervisor> findByAvailability(String availability);

    long countByAvailability(String availability);
}