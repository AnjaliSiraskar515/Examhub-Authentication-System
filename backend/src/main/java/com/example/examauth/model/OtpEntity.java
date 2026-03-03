package com.example.examauth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_store")
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String phone;
    private String otp;
    private LocalDateTime expiryTime;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean verified = false;

    public OtpEntity() {
    }

    public OtpEntity(String email, String phone, String otp, LocalDateTime expiryTime) {
        this.email = email;
        this.phone = phone;
        this.otp = otp;
        this.expiryTime = expiryTime;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getOtp() {
        return otp;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
