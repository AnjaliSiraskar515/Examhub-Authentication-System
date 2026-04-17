package com.example.examauth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "student_backlogs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"studentId", "subjectId"})
})
public class StudentBacklog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId; // Foreign Key to User.userId

    @Column(nullable = false)
    private Long subjectId; // Foreign Key to Subject.id

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean cleared = false;

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

    public Boolean getCleared() {
        return cleared;
    }

    public void setCleared(Boolean cleared) {
        this.cleared = cleared;
    }
}
