package com.example.examauth.repo;

import com.example.examauth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email
    // Find user by email
    Optional<User> findByEmail(String email);

    // ✅ NEW: Find FIRST user by email (handles duplicates safely)
    Optional<User> findFirstByEmail(String email);

    Optional<User> findFirstByEmailAndRole(String email, String role);

    // ✅ NEW: Find user by PRN (for student login)
    Optional<User> findByPrn(String prn);

    // ✅ NEW: Count students for dashboard
    long countByRole(String role);

    // ✅ NEW: List students by role
    java.util.List<User> findByRole(String role);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u LEFT JOIN FETCH u.college WHERE u.role = :role")
    java.util.List<User> findByRoleWithCollege(@org.springframework.data.repository.query.Param("role") String role);

    // ✅ NEW: Count by biometric verification status
    long countByBiometricVerified(Boolean verified);

    // ✅ NEW: Count by role and biometric verification status
    // ✅ NEW: Count by role and biometric verification status
    long countByRoleAndBiometricVerified(String role, Boolean verified);

    // ✅ NEW: Find user by phone number (for mobile login)
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findFirstByPhoneNumberAndRole(String phoneNumber, String role);

    // ✅ NEW: Find user by username
    Optional<User> findByUsername(String username);

    // ✅ Communication Hub: Find supervisors by institution code and role
    java.util.List<User> findByInstitutionCodeAndRole(String institutionCode, String role);

    // ✅ Communication Hub: Find HEAD supervisors by institution code
    java.util.List<User> findByInstitutionCodeAndRoleAndSupervisorType(String institutionCode, String role, String supervisorType);
}
