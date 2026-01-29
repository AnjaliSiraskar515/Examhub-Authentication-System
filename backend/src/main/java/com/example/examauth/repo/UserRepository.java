package com.example.examauth.repo;

import com.example.examauth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // ✅ NEW: Find user by username (for student login)
    Optional<User> findByUsername(String username);

    // ✅ NEW: Count students for dashboard
    long countByRole(String role);

    // ✅ NEW: List students by role
    java.util.List<User> findByRole(String role);
}
