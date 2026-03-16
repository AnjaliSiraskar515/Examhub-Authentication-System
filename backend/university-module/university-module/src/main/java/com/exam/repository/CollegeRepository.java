package com.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.exam.entity.College;   

public interface CollegeRepository extends JpaRepository<College, Long> {
}