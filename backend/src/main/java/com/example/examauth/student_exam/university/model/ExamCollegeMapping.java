package com.example.examauth.student_exam.university.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Maps a published UniversityExam to each affiliated college it covers.
 * Each college gets its own Head Supervisor assignment and status tracking.
 * Additional layer — does NOT replace UniversityExam.collegeId.
 */
@Entity
@Table(
    name = "exam_college_mappings",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_exam_college",
        columnNames = {"exam_id", "college_id"}
    ),
    indexes = {
        @Index(name = "idx_ecm_exam_id",         columnList = "exam_id"),
        @Index(name = "idx_ecm_college_id",       columnList = "college_id"),
        @Index(name = "idx_ecm_supervisor_id",    columnList = "head_supervisor_id"),
        @Index(name = "idx_ecm_status",           columnList = "status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamCollegeMapping {

    // ─────────────────────────────────────────────────────────────────────────
    // Status Enum — replaces raw String to prevent typos and enable type safety
    // ─────────────────────────────────────────────────────────────────────────
    public enum MappingStatus {
        /** Assigned but Head Supervisor has not yet configured halls */
        PENDING,
        /** Head Supervisor has added at least one hall with capacity */
        HALLS_CONFIGURED,
        /** Seat allocation algorithm has run and all seats are assigned */
        SEATS_GENERATED,
        /** Exam has been conducted at this college */
        COMPLETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → new_university_exams.id — the parent exam */
    @Column(name = "exam_id", nullable = false)
    private Long examId;

    /** FK → colleges.id — which college this mapping is for */
    @Column(name = "college_id", nullable = false)
    private Long collegeId;

    /** Denormalized for fast display without joins */
    @Column(name = "college_name")
    private String collegeName;

    /** The Head Supervisor responsible for this college's exam conduct */
    @Column(name = "head_supervisor_id")
    private Long headSupervisorId;

    @Column(name = "head_supervisor_name")
    private String headSupervisorName;

    /**
     * Lifecycle status stored as a named enum string.
     * Default: PENDING — assigned but halls not yet configured.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MappingStatus status = MappingStatus.PENDING;

    /** Total students registered & approved at this college for this exam */
    @Column(name = "total_students")
    private Integer totalStudents = 0;

    /** Total seat capacity across all halls configured for this mapping */
    @Column(name = "total_capacity")
    private Integer totalCapacity = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = MappingStatus.PENDING;
        if (totalStudents == null) totalStudents = 0;
        if (totalCapacity == null) totalCapacity = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
