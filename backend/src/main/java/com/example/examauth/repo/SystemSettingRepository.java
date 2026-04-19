package com.example.examauth.repo;

import com.example.examauth.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findByKey(String key);

    Optional<SystemSetting> findTopByOrderByUpdatedAtDesc();
}
