package com.example.examauth.controller;

import com.example.examauth.model.CommunicationMessage;
import com.example.examauth.model.Institution;
import com.example.examauth.model.User;
import com.example.examauth.repo.CommunicationMessageRepository;
import com.example.examauth.repo.InstitutionRepository;
import com.example.examauth.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class CommunicationController {

    private final CommunicationMessageRepository communicationMessageRepository;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;

    public CommunicationController(
            CommunicationMessageRepository communicationMessageRepository,
            UserRepository userRepository,
            InstitutionRepository institutionRepository) {
        this.communicationMessageRepository = communicationMessageRepository;
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
    }

    @PostMapping("/api/admin/communication/send")
    public ResponseEntity<?> sendMessage(Authentication authentication, @RequestBody Map<String, String> body) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return ResponseEntity.status(401).body(response(false, "Unauthorized", Map.of()));
        }

        String subject = value(body.get("subject"));
        String message = value(body.get("message"));
        String recipient = value(body.get("recipient"));
        String priority = normalizePriority(value(body.get("priority")));
        String type = normalizeType(value(body.get("type")));

        if (subject.isBlank() || message.isBlank() || recipient.isBlank()) {
            return ResponseEntity.badRequest().body(response(false, "Subject, message and recipient are required", Map.of()));
        }

        String senderEmail = authentication.getName();
        String receiverEmail = resolveInstitutionAdminEmail(recipient);

        CommunicationMessage msg = new CommunicationMessage();
        msg.setSenderRole("SUPER_ADMIN");
        msg.setReceiverRole("UNIVERSITY_ADMIN");
        msg.setSenderEmail(senderEmail);
        msg.setReceiverEmail(receiverEmail);
        msg.setReceiverInstitutionCode(recipient);
        msg.setSubject(subject);
        msg.setMessage(message);
        msg.setPriority(priority);
        msg.setType(type);
        msg.setStatus("SENT");
        msg.setReplyAllowed("GENERAL".equals(type)); // Only GENERAL allows reply
        msg.setAcknowledged(false); // WARNING uses this
        msg.setParentMessageId(null);
        msg.setCreatedAt(LocalDateTime.now());
        CommunicationMessage saved = communicationMessageRepository.save(msg);

        return ResponseEntity.ok(response(true, "Communication sent successfully", saved));
    }

    @GetMapping("/api/admin/communication/history")
    public ResponseEntity<?> getAdminHistory(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return ResponseEntity.status(401).body(response(false, "Unauthorized", List.of()));
        }
        String adminEmail = authentication.getName();
        List<CommunicationMessage> history = communicationMessageRepository
                .findBySenderEmailOrReceiverEmailOrderByCreatedAtDesc(adminEmail, adminEmail);
        return ResponseEntity.ok(response(true, "Communication history fetched successfully", history));
    }

    @GetMapping("/api/university/communication/inbox")
    public ResponseEntity<?> getUniversityInbox(Authentication authentication) {
        String institutionCode = resolveInstitutionCode(authentication);
        if (institutionCode == null || institutionCode.isBlank()) {
            return ResponseEntity.ok(response(true, "Institution code not found for current user, returning empty inbox", List.of()));
        }

        List<CommunicationMessage> inbox = communicationMessageRepository
                .findByReceiverRoleAndReceiverInstitutionCodeOrderByCreatedAtDesc("UNIVERSITY_ADMIN", institutionCode);

        return ResponseEntity.ok(response(true, "Inbox fetched successfully", inbox));
    }

    @PostMapping("/api/university/communication/mark-read")
    public ResponseEntity<?> markRead(Authentication authentication, @RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        if (idObj == null) {
            return ResponseEntity.badRequest().body(response(false, "Message id is required", Map.of()));
        }

        Long messageId;
        try {
            messageId = Long.valueOf(String.valueOf(idObj));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(response(false, "Invalid message id", Map.of()));
        }

        String institutionCode = resolveInstitutionCode(authentication);
        if (institutionCode == null || institutionCode.isBlank()) {
            return ResponseEntity.status(404).body(response(false, "Institution code not found for current user", Map.of()));
        }

        CommunicationMessage msg = communicationMessageRepository.findById(messageId).orElse(null);
        if (msg == null) {
            return ResponseEntity.status(404).body(response(false, "Message not found", Map.of()));
        }
        if (!"UNIVERSITY_ADMIN".equalsIgnoreCase(msg.getReceiverRole())
                || !institutionCode.equalsIgnoreCase(value(msg.getReceiverInstitutionCode()))) {
            return ResponseEntity.status(403).body(response(false, "Message does not belong to current institution", Map.of()));
        }

        if ("SENT".equalsIgnoreCase(value(msg.getStatus()))) {
            msg.setStatus("READ");
        }
        communicationMessageRepository.save(msg);
        return ResponseEntity.ok(response(true, "Message marked as read", msg));
    }

    @PostMapping("/api/university/communication/acknowledge")
    public ResponseEntity<?> acknowledgeMessage(Authentication authentication, @RequestBody Map<String, Object> body) {
        Long messageId = parseMessageId(body.get("id"));
        if (messageId == null) {
            return ResponseEntity.badRequest().body(response(false, "Message id is required", Map.of()));
        }

        String institutionCode = resolveInstitutionCode(authentication);
        if (institutionCode == null || institutionCode.isBlank()) {
            return ResponseEntity.status(404).body(response(false, "Institution code not found for current user", Map.of()));
        }

        CommunicationMessage msg = communicationMessageRepository.findById(messageId).orElse(null);
        if (msg == null) {
            return ResponseEntity.status(404).body(response(false, "Message not found", Map.of()));
        }
        if (!"UNIVERSITY_ADMIN".equalsIgnoreCase(value(msg.getReceiverRole()))
                || !institutionCode.equalsIgnoreCase(value(msg.getReceiverInstitutionCode()))) {
            return ResponseEntity.status(403).body(response(false, "Message does not belong to current institution", Map.of()));
        }
        String msgType = value(msg.getType());
        if (msgType.isBlank() || !"WARNING".equalsIgnoreCase(msgType)) {
            return ResponseEntity.status(400).body(response(false, "Acknowledge is only for WARNING messages", Map.of()));
        }

        msg.setAcknowledged(true);
        if ("SENT".equalsIgnoreCase(value(msg.getStatus()))) {
            msg.setStatus("READ");
        }
        communicationMessageRepository.save(msg);
        return ResponseEntity.ok(response(true, "Message acknowledged", msg));
    }

    @PostMapping("/api/messages/request-reply")
    public ResponseEntity<?> requestReplyPermission(Authentication authentication, @RequestBody Map<String, Object> body) {
        Long messageId = parseMessageId(body.get("id"));
        if (messageId == null) {
            return ResponseEntity.badRequest().body(response(false, "Valid message id is required", Map.of()));
        }

        String institutionCode = resolveInstitutionCode(authentication);
        if (institutionCode == null || institutionCode.isBlank()) {
            return ResponseEntity.status(404).body(response(false, "Institution code not found for current user", Map.of()));
        }

        CommunicationMessage msg = communicationMessageRepository.findById(messageId).orElse(null);
        if (msg == null) {
            return ResponseEntity.status(404).body(response(false, "Message not found", Map.of()));
        }
        if (!"UNIVERSITY_ADMIN".equalsIgnoreCase(value(msg.getReceiverRole()))
                || !institutionCode.equalsIgnoreCase(value(msg.getReceiverInstitutionCode()))) {
            return ResponseEntity.status(403).body(response(false, "Message does not belong to current institution", Map.of()));
        }

        // Reply workflow applies only to GENERAL type (null/empty treated as GENERAL for backward compat)
        String msgType = value(msg.getType());
        if (msgType.isBlank()) msgType = "GENERAL";
        else msgType = msgType.toUpperCase();
        if (!"GENERAL".equals(msgType)) {
            return ResponseEntity.status(400).body(response(false, "Reply request is not allowed for this message type", Map.of()));
        }

        String currentStatus = value(msg.getStatus()).toUpperCase();
        if ("APPROVED".equals(currentStatus) && Boolean.TRUE.equals(msg.getReplyAllowed())) {
            return ResponseEntity.ok(response(true, "Reply already approved for this message", msg));
        }
        if ("REQUESTED".equals(currentStatus)) {
            return ResponseEntity.ok(response(true, "Reply permission already requested", msg));
        }

        msg.setStatus("REQUESTED");
        communicationMessageRepository.save(msg);
        return ResponseEntity.ok(response(true, "Reply permission requested", msg));
    }

    @GetMapping("/api/admin/communication/reply-requests")
    public ResponseEntity<?> getReplyRequests() {
        List<CommunicationMessage> requested = communicationMessageRepository.findByStatusOrderByCreatedAtDesc("REQUESTED");
        return ResponseEntity.ok(response(true, "Reply requests fetched successfully", requested));
    }

    @PostMapping("/api/admin/communication/reply-requests/{id}/approve")
    public ResponseEntity<?> approveReplyRequest(@PathVariable Long id) {
        CommunicationMessage msg = communicationMessageRepository.findById(id).orElse(null);
        if (msg == null) {
            return ResponseEntity.status(404).body(response(false, "Message not found", Map.of()));
        }
        msg.setReplyAllowed(true);
        msg.setStatus("APPROVED");
        communicationMessageRepository.save(msg);
        return ResponseEntity.ok(response(true, "Reply request approved", msg));
    }

    @PostMapping("/api/admin/communication/reply-requests/{id}/reject")
    public ResponseEntity<?> rejectReplyRequest(@PathVariable Long id) {
        CommunicationMessage msg = communicationMessageRepository.findById(id).orElse(null);
        if (msg == null) {
            return ResponseEntity.status(404).body(response(false, "Message not found", Map.of()));
        }
        msg.setReplyAllowed(false);
        msg.setStatus("REJECTED");
        communicationMessageRepository.save(msg);
        return ResponseEntity.ok(response(true, "Reply request rejected", msg));
    }

    @PostMapping("/api/messages/reply")
    public ResponseEntity<?> sendUniversityReply(Authentication authentication, @RequestBody Map<String, Object> body) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return ResponseEntity.status(401).body(response(false, "Unauthorized", Map.of()));
        }
        Long parentMessageId = parseMessageId(body.get("parentMessageId"));
        String replyMessage = body.get("message") == null ? "" : String.valueOf(body.get("message")).trim();
        if (parentMessageId == null || replyMessage.isBlank()) {
            return ResponseEntity.badRequest().body(response(false, "parentMessageId and message are required", Map.of()));
        }

        String institutionCode = resolveInstitutionCode(authentication);
        if (institutionCode == null || institutionCode.isBlank()) {
            return ResponseEntity.status(404).body(response(false, "Institution code not found for current user", Map.of()));
        }

        CommunicationMessage parent = communicationMessageRepository.findById(parentMessageId).orElse(null);
        if (parent == null) {
            return ResponseEntity.status(404).body(response(false, "Parent message not found", Map.of()));
        }
        if (!institutionCode.equalsIgnoreCase(value(parent.getReceiverInstitutionCode()))) {
            return ResponseEntity.status(403).body(response(false, "Reply not allowed for this institution", Map.of()));
        }
        // Reply allowed only for GENERAL type with approval (null/empty treated as GENERAL for backward compat)
        String parentType = value(parent.getType());
        if (parentType.isBlank()) parentType = "GENERAL";
        else parentType = parentType.toUpperCase();
        if (!"GENERAL".equals(parentType)) {
            return ResponseEntity.status(403).body(response(false, "Reply not allowed for this message type", Map.of()));
        }
        if (!Boolean.TRUE.equals(parent.getReplyAllowed())) {
            return ResponseEntity.status(403).body(response(false, "Reply not allowed", Map.of()));
        }

        CommunicationMessage reply = new CommunicationMessage();
        reply.setSenderRole("UNIVERSITY_ADMIN");
        reply.setReceiverRole(value(parent.getSenderRole()));
        reply.setSenderEmail(authentication.getName());
        reply.setReceiverEmail(value(parent.getSenderEmail()));
        reply.setReceiverInstitutionCode(value(parent.getReceiverInstitutionCode()));
        reply.setSubject("Re: " + value(parent.getSubject()));
        reply.setMessage(replyMessage);
        reply.setPriority("NORMAL");
        reply.setStatus("SENT");
        reply.setReplyAllowed(false);
        reply.setParentMessageId(parentMessageId);
        reply.setCreatedAt(LocalDateTime.now());
        CommunicationMessage saved = communicationMessageRepository.save(reply);

        return ResponseEntity.ok(response(true, "Reply sent successfully", saved));
    }

    @DeleteMapping("/api/admin/communication/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id) {
        if (!communicationMessageRepository.existsById(id)) {
            return ResponseEntity.status(404).body(response(false, "Message not found", Map.of()));
        }
        communicationMessageRepository.deleteById(id);
        return ResponseEntity.ok(response(true, "Message deleted successfully", Map.of("id", id)));
    }

    @DeleteMapping("/api/university/communication/{id}")
    public ResponseEntity<?> deleteUniversityMessage(Authentication authentication, @PathVariable Long id) {
        String institutionCode = resolveInstitutionCode(authentication);
        if (institutionCode == null || institutionCode.isBlank()) {
            return ResponseEntity.status(404).body(response(false, "Institution code not found for current user", Map.of()));
        }

        CommunicationMessage msg = communicationMessageRepository.findById(id).orElse(null);
        if (msg == null) {
            return ResponseEntity.status(404).body(response(false, "Message not found", Map.of()));
        }
        if (!"UNIVERSITY_ADMIN".equalsIgnoreCase(value(msg.getReceiverRole()))
                || !institutionCode.equalsIgnoreCase(value(msg.getReceiverInstitutionCode()))) {
            return ResponseEntity.status(403).body(response(false, "Message does not belong to current institution", Map.of()));
        }

        communicationMessageRepository.deleteById(id);
        return ResponseEntity.ok(response(true, "Message deleted successfully", Map.of("id", id)));
    }

    private String resolveInstitutionCode(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        String email = authentication.getName();
        User user = userRepository.findFirstByEmail(email).orElse(null);
        if (user == null) {
            return null;
        }
        if (user.getInstitutionCode() != null && !user.getInstitutionCode().isBlank()) {
            return user.getInstitutionCode();
        }

        Institution byContact = institutionRepository.findFirstByContactEmail(email).orElse(null);
        if (byContact != null && byContact.getInstitutionCode() != null) {
            user.setInstitutionCode(byContact.getInstitutionCode());
            userRepository.save(user);
            return byContact.getInstitutionCode();
        }

        Institution byAdmin = institutionRepository.findFirstByAdminEmail(email).orElse(null);
        if (byAdmin != null && byAdmin.getInstitutionCode() != null) {
            user.setInstitutionCode(byAdmin.getInstitutionCode());
            userRepository.save(user);
            return byAdmin.getInstitutionCode();
        }
        return null;
    }

    private String normalizePriority(String value) {
        if (value == null || value.isBlank()) return "NORMAL";
        String upper = value.trim().toUpperCase();
        if ("LOW".equals(upper) || "NORMAL".equals(upper) || "URGENT".equals(upper)) {
            return upper;
        }
        return "NORMAL";
    }

    private String normalizeType(String value) {
        if (value == null || value.isBlank()) return "GENERAL";
        String upper = value.trim().toUpperCase();
        if ("GENERAL".equals(upper) || "WARNING".equals(upper) || "BROADCAST".equals(upper)) {
            return upper;
        }
        return "GENERAL";
    }

    private String resolveInstitutionAdminEmail(String institutionCode) {
        if (institutionCode == null || institutionCode.isBlank()) {
            return "";
        }
        Institution institution = institutionRepository.findFirstByInstitutionCode(institutionCode).orElse(null);
        if (institution == null) {
            return "";
        }
        if (institution.getAdminEmail() != null && !institution.getAdminEmail().isBlank()) {
            return institution.getAdminEmail().trim();
        }
        if (institution.getContactEmail() != null && !institution.getContactEmail().isBlank()) {
            return institution.getContactEmail().trim();
        }
        return "";
    }

    private Long parseMessageId(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(raw));
        } catch (Exception ex) {
            return null;
        }
    }

    private String value(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private Map<String, Object> response(boolean success, String message, Object data) {
        return Map.of(
                "success", success,
                "message", message,
                "data", data);
    }
}
