package com.example.examauth.student_exam.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a physical exam hall configured by the Head Supervisor
 * for a specific exam at a specific college.
 * Each hall has a unique prefix (e.g. "A") used for seat numbering,
 * a capacity, and optionally an assigned exam-level supervisor.
 */
@Entity
@Table(
    name = "exam_halls",
    indexes = {
        @Index(name = "idx_eh_exam_id",         columnList = "exam_id"),
        @Index(name = "idx_eh_college_id",      columnList = "college_id"),
        @Index(name = "idx_eh_supervisor_id",   columnList = "created_by_supervisor_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamHall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → new_university_exams.id */
    @Column(name = "exam_id", nullable = false)
    private Long examId;

    /** FK → colleges.id — the college where this hall is located */
    @Column(name = "college_id", nullable = false)
    private Long collegeId;

    /**
     * Dedicated prefix for seat numbering.
     * Separate from hallName — e.g. prefix="A", hallName="Lecture Hall 101"
     * Seats generated as: A-001, A-002 ... A-040
     * Must be unique within (exam_id, college_id).
     */
    @Column(name = "hall_prefix", nullable = false, length = 10)
    private String hallPrefix;

    /** Full descriptive name e.g. "Lecture Hall 101", "Computer Lab 2" */
    @Column(name = "hall_name", nullable = false)
    private String hallName;

    /** Maximum number of students this hall can accommodate */
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    /**
     * Optional: exam-level supervisor for this specific hall.
     * Distinct from the Head Supervisor who manages the whole college.
     * FK → users.userId (EXAM type supervisor)
     */
    @Column(name = "exam_supervisor_id")
    private Long examSupervisorId;

    @Column(name = "exam_supervisor_name")
    private String examSupervisorName;

    /** Denormalized: count of students actually assigned to this hall */
    @Column(name = "seats_assigned")
    private Integer seatsAssigned = 0;

    /**
     * FK → users.userId — which Head Supervisor created this hall entry.
     * Enforces that only the responsible supervisor can create halls.
     */
    @Column(name = "created_by_supervisor_id")
    private Long createdBySupervisorId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (seatsAssigned == null) seatsAssigned = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
