package com.example.examauth.repo;

import com.example.examauth.model.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    Optional<Institution> findFirstByInstitutionCode(String institutionCode);

    Optional<Institution> findFirstByName(String name);

    Optional<Institution> findFirstByContactEmail(String email);

    Optional<Institution> findFirstByAdminEmail(String email);

    long countByStatusIgnoreCase(String status);
}
