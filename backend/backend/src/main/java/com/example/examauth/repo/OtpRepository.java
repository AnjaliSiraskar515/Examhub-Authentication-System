package com.example.examauth.repo;

import com.example.examauth.model.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    Optional<OtpEntity> findByEmail(String email);
    Optional<OtpEntity> findByPhone(String phone);
}
