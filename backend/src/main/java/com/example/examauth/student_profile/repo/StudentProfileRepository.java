package com.example.examauth.student_profile.repo;

import com.example.examauth.student_profile.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    // No custom queries needed for this module.
}
