package com.example.examauth.exam_registration;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity(name = "ExamRegistrationModule")
@Table(name = "exam_registrations_module")
public class ExamRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    private Long examId;

    private LocalDateTime registrationDate;

    private String status; // REGISTERED, CANCELLED

    private boolean hallTicketGenerated;

    public ExamRegistration() {
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHallTicketGenerated() {
        return hallTicketGenerated;
    }

    public void setHallTicketGenerated(boolean hallTicketGenerated) {
        this.hallTicketGenerated = hallTicketGenerated;
    }
}
