package com.example.examauth.repo;

import com.example.examauth.model.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QrRepository extends JpaRepository<QrCode, Long> {
    Optional<QrCode> findByHashedToken(String hashedToken);
}
