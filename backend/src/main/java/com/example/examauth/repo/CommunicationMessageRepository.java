package com.example.examauth.repo;

import com.example.examauth.model.CommunicationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunicationMessageRepository extends JpaRepository<CommunicationMessage, Long> {
    List<CommunicationMessage> findBySenderRoleOrderByCreatedAtDesc(String senderRole);

    List<CommunicationMessage> findByReceiverRoleAndReceiverInstitutionCodeOrderByCreatedAtDesc(
            String receiverRole,
            String receiverInstitutionCode);

    List<CommunicationMessage> findByStatusOrderByCreatedAtDesc(String status);

    List<CommunicationMessage> findBySenderRoleOrReceiverRoleOrderByCreatedAtDesc(String senderRole, String receiverRole);

    List<CommunicationMessage> findBySenderEmailOrReceiverEmailOrderByCreatedAtDesc(String senderEmail, String receiverEmail);

    // Fetch university admin inbox: messages where receiver is UNIVERSITY_ADMIN
    // matched by institution code OR directly by admin email (for legacy messages with empty institution code)
    @Query("SELECT m FROM CommunicationMessage m WHERE m.receiverRole = 'UNIVERSITY_ADMIN' " +
           "AND (m.receiverInstitutionCode = :institutionCode OR m.receiverEmail = :adminEmail) " +
           "ORDER BY m.createdAt DESC NULLS LAST")
    List<CommunicationMessage> findUniversityAdminInbox(
            @Param("institutionCode") String institutionCode,
            @Param("adminEmail") String adminEmail);

    // Fetch ALL messages involving the university admin — both received and sent
    @Query("SELECT m FROM CommunicationMessage m WHERE " +
           "(m.receiverRole = 'UNIVERSITY_ADMIN' AND (m.receiverInstitutionCode = :institutionCode OR m.receiverEmail = :adminEmail)) " +
           "OR (m.senderRole = 'UNIVERSITY_ADMIN' AND m.senderEmail = :adminEmail) " +
           "ORDER BY m.createdAt DESC NULLS LAST")
    List<CommunicationMessage> findUniversityAdminAllMessages(
            @Param("institutionCode") String institutionCode,
            @Param("adminEmail") String adminEmail);
}
