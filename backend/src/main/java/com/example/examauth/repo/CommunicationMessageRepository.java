package com.example.examauth.repo;

import com.example.examauth.model.CommunicationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunicationMessageRepository extends JpaRepository<CommunicationMessage, Long> {
    List<CommunicationMessage> findBySenderRoleOrderByCreatedAtDesc(String senderRole);

    List<CommunicationMessage> findByReceiverRoleAndReceiverInstitutionCodeOrderByCreatedAtDesc(
            String receiverRole,
            String receiverInstitutionCode);

    List<CommunicationMessage> findByStatusOrderByCreatedAtDesc(String status);

    List<CommunicationMessage> findBySenderRoleOrReceiverRoleOrderByCreatedAtDesc(String senderRole, String receiverRole);

    List<CommunicationMessage> findBySenderEmailOrReceiverEmailOrderByCreatedAtDesc(String senderEmail, String receiverEmail);
}
