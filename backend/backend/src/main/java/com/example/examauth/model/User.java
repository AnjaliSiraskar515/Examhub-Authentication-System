package com.example.examauth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;
    private String email;

    // ✅ New field added here
    @Column(unique = true)
    private String username;  // For student login via username/roll number

    private String password;
    private String role;
    private String status;

    @Column(length = 2000)
    private String biometricHash;

    private String photoPath;

    // Document paths for profile completion
    private String aadharPath;
    private String marks10Path;
    private String marks12Path;
    private String ugPath;
    private String pgPath;
    private String biometricPath;

    private String phoneNumber;
    private String passportPhotoPath;
    private String idProofPath;

    // Profile completion status
    private Boolean profileCompleted = false;

    // ===========================================================
    // Getters and Setters
    // ===========================================================

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long id) {
        this.userId = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String e) {
        this.email = e;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    // ✅ New getter/setter for username
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String p) {
        this.password = p;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String r) {
        this.role = r;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setBiometricHash(String h) {
        this.biometricHash = h;
    }

    public String getBiometricHash() {
        return this.biometricHash;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getAadharPath() {
        return aadharPath;
    }

    public void setAadharPath(String aadharPath) {
        this.aadharPath = aadharPath;
    }

    public String getMarks10Path() {
        return marks10Path;
    }

    public void setMarks10Path(String marks10Path) {
        this.marks10Path = marks10Path;
    }

    public String getMarks12Path() {
        return marks12Path;
    }

    public void setMarks12Path(String marks12Path) {
        this.marks12Path = marks12Path;
    }

    public String getUgPath() {
        return ugPath;
    }

    public void setUgPath(String ugPath) {
        this.ugPath = ugPath;
    }

    public String getPgPath() {
        return pgPath;
    }

    public void setPgPath(String pgPath) {
        this.pgPath = pgPath;
    }

    public String getBiometricPath() {
        return biometricPath;
    }

    public void setBiometricPath(String biometricPath) {
        this.biometricPath = biometricPath;
    }

    public Boolean getProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(Boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassportPhotoPath() {
        return passportPhotoPath;
    }

    public void setPassportPhotoPath(String passportPhotoPath) {
        this.passportPhotoPath = passportPhotoPath;
    }

    public String getIdProofPath() {
        return idProofPath;
    }

    public void setIdProofPath(String idProofPath) {
        this.idProofPath = idProofPath;
    }
}
