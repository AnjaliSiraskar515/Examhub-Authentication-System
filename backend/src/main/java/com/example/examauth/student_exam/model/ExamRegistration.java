package com.example.examauth.student_exam.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_registrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "prn", "examId" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long examId;

    @Column(nullable = false)
    private String prn;

    private String fullName;
    private String course;
    private String year;
    private String institutionName;
    private String examSession;

    private LocalDate appliedDate;

    @Column(name = "total_fee")
    private Double totalFee;

    // ========== NEW PROFESSIONAL FIELDS ==========

    @Column(name = "hall_ticket_released")
    private Boolean hallTicketReleased = false;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    /**
     * List of subjects selected by student for this exam
     * Stored as JSON in database
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_subjects", columnDefinition = "json", updatable = false)
    private List<String> selectedSubjects = new ArrayList<>();

    /**
     * Type of exam: REGULAR, BACKLOG, IMPROVEMENT, etc.
     */
    @Column(name = "exam_type", length = 50)
    private String examType;

    /**
     * Student declaration acceptance (terms & conditions)
     */
    @Column(name = "declaration_accepted")
    private Boolean declarationAccepted = false;

    /**
     * Payment status for registration fee
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Registration approval status
     * APPLIED - Initial state after submission
     * APPROVED - Verified and approved by admin
     * REJECTED - Rejected (legacy from old enum)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false)
    private RegistrationStatus registrationStatus = RegistrationStatus.APPLIED;

    /**
     * Timestamp when registration was submitted
     * Auto-set on entity creation
     */
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    // ========== LIFECYCLE CALLBACKS ==========

    /**
     * Auto-set submittedAt timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        // Set defaults if not provided
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
        if (registrationStatus == null) {
            registrationStatus = RegistrationStatus.APPLIED;
        }
        if (declarationAccepted == null) {
            declarationAccepted = false;
        }
        if (selectedSubjects == null) {
            selectedSubjects = new ArrayList<>();
        }
    }

    // ========== ENUMS ==========

    /**
     * Payment status for exam registration fee
     */
    public enum PaymentStatus {
        PENDING, // Payment not yet completed
        PAID // Payment successfully processed
    }

    /**
     * Registration approval status
     * Enhanced to include APPLIED state for new workflow
     */
    public enum RegistrationStatus {
        APPLIED, // Initial state after form submission
        APPROVED, // Verified and approved by admin
        REJECTED, // Rejected (kept for backward compatibility)
        PENDING // Legacy status (kept for backward compatibility)
    }
}
