package com.example.examauth.student_exam.university.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity class for exam_eligible_students table
 * Stores students eligible for specific exam sessions
 * Uploaded by University Admin via Excel
 */
@Entity
@Table(name = "exam_eligible_students", uniqueConstraints = @UniqueConstraint(name = "unique_prn_exam_session", columnNames = {
        "prn_number", "exam_session" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamEligibleStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "university_id", nullable = false)
    private Integer universityId;

    @Column(name = "prn_number", nullable = false, length = 50)
    private String prnNumber;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "semester", length = 20)
    private String semester;

    @Column(name = "exam_session", length = 100)
    private String examSession;

    // Store as JSON string in database, convert to/from List<String>
    @Column(name = "eligible_subjects", columnDefinition = "JSON")
    private String eligibleSubjectsJson;

    @Column(name = "fee_status", length = 50)
    private String feeStatus = "Pending";

    @Column(name = "eligibility_status", length = 50)
    private String eligibilityStatus = "Eligible";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Transient field for easier manipulation
    @Transient
    private List<String> eligibleSubjects;

    /**
     * Convert List<String> to JSON string before persisting
     */
    public void setEligibleSubjects(List<String> subjects) {
        this.eligibleSubjects = subjects;
        if (subjects != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                this.eligibleSubjectsJson = mapper.writeValueAsString(subjects);
            } catch (JsonProcessingException e) {
                this.eligibleSubjectsJson = "[]";
            }
        } else {
            this.eligibleSubjectsJson = "[]";
        }
    }

    /**
     * Convert JSON string to List<String> after loading
     */
    public List<String> getEligibleSubjects() {
        if (eligibleSubjects == null && eligibleSubjectsJson != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                eligibleSubjects = mapper.readValue(
                        eligibleSubjectsJson,
                        new TypeReference<List<String>>() {
                        });
            } catch (JsonProcessingException e) {
                eligibleSubjects = new ArrayList<>();
            }
        }
        return eligibleSubjects != null ? eligibleSubjects : new ArrayList<>();
    }
}
