package com.example.examauth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Fixing database column types for exam_registrations...");
            jdbcTemplate.execute("ALTER TABLE exam_registrations MODIFY payment_status VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE exam_registrations MODIFY registration_status VARCHAR(50)");
            log.info("Successfully fixed database columns!");
        } catch (Exception e) {
            log.warn("Database fix could not be applied (maybe already fixed?): " + e.getMessage());
        }
    }
}
