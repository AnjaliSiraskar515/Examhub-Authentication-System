package com.example.examauth.repo;

import com.example.examauth.model.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    Optional<OtpEntity> findByEmail(String email);

    List<OtpEntity> findByPhone(String phone);
}
