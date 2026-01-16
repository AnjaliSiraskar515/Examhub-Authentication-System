package com.example.examauth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "fraud_logs")
public class FraudLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    private Long userId;
    private Long examId;

    @Column(length = 2000)
    private String description;

    private String severity;
    private java.sql.Timestamp detectedOn;
    private Boolean escalatedToSuperAdmin;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long u) {
        this.userId = u;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long e) {
        this.examId = e;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String s) {
        this.severity = s;
    }

    public java.sql.Timestamp getDetectedOn() {
        return detectedOn;
    }

    public void setDetectedOn(java.sql.Timestamp t) {
        this.detectedOn = t;
    }

    public Boolean getEscalatedToSuperAdmin() {
        return escalatedToSuperAdmin;
    }

    public void setEscalatedToSuperAdmin(Boolean b) {
        this.escalatedToSuperAdmin = b;
    }
}
