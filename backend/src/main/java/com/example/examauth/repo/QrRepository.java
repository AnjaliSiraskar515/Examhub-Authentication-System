package com.example.examauth.repo;

import com.example.examauth.model.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QrRepository extends JpaRepository<QrCode, Long> {
    Optional<QrCode> findByHashedToken(String hashedToken);

    // ✅ NEW: Count used (verified) tokens
    long countByUsed(boolean used);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT q.studentId) FROM QrCode q WHERE q.used = :used")
    long countDistinctStudentIdByUsed(@org.springframework.data.repository.query.Param("used") boolean used);

    // Count today's scans
    long countByUsedAndIssuedAtAfter(boolean used, java.time.LocalDateTime issuedAt);
}
