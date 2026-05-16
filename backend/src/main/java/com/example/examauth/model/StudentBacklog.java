package com.example.examauth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "student_backlogs")
public class StudentBacklog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId; // Foreign Key to User.userId

    @Column(nullable = true)
    private Long subjectId; // Foreign Key to Subject.id

    @Column(nullable = true)
    private String subjectName; // E.g., "Computer Networks"

    @Column(nullable = true)
    private String semester; // E.g., "4"

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean cleared = false; // Kept for backward compatibility

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getStatus() {
        return (cleared != null && cleared) ? "CLEARED" : "PENDING";
    }

    public Boolean getCleared() {
        return cleared;
    }

    public void setCleared(Boolean cleared) {
        this.cleared = cleared;
    }
}
