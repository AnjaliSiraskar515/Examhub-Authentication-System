package com.example.examauth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "qr_codes")
public class QRCodeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long qrId;

    private Long userId;
    private Long examId;

    @Column(length = 2000)
    private String token;

    private java.sql.Timestamp generatedOn;

    public Long getQrId() {
        return qrId;
    }

    public void setQrId(Long qrId) {
        this.qrId = qrId;
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

    public String getToken() {
        return token;
    }

    public void setToken(String t) {
        this.token = t;
    }

    public java.sql.Timestamp getGeneratedOn() {
        return generatedOn;
    }

    public void setGeneratedOn(java.sql.Timestamp generatedOn) {
        this.generatedOn = generatedOn;
    }
}
