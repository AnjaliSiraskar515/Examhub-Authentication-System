package com.example.examauth.repo;

import com.example.examauth.model.College;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long> {
    Optional<College> findByNameIgnoreCase(String name);
}
