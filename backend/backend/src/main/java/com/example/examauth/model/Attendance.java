package com.example.examauth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long examId;
    private Long studentId;
    private Long supervisorId;
    private java.sql.Timestamp markedOn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long e) {
        this.examId = e;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long s) {
        this.studentId = s;
    }

    public Long getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(Long s) {
        this.supervisorId = s;
    }

    public java.sql.Timestamp getMarkedOn() {
        return markedOn;
    }

    public void setMarkedOn(java.sql.Timestamp markedOn) {
        this.markedOn = markedOn;
    }
}
