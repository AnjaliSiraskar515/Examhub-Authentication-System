package com.example.examauth.repo;

import com.example.examauth.model.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    java.util.List<OtpEntity> findByEmail(String email);

    java.util.List<OtpEntity> findByPhone(String phone);

    void deleteByEmail(String email);

    void deleteByPhone(String phone);
}
