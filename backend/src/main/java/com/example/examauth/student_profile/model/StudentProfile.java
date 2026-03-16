package com.example.examauth.student_profile.model;

import jakarta.persistence.*;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String prn;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String course;

    @Column(nullable = false)
    private String year;

    private String photoPath;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private boolean profileLocked;

    // -------------------------------------------------------
    // Constructors
    // -------------------------------------------------------

    public StudentProfile() {
    }

    public StudentProfile(String prn, String fullName, String course, String year,
            boolean verified, boolean profileLocked) {
        this.prn = prn;
        this.fullName = fullName;
        this.course = course;
        this.year = year;
        this.verified = verified;
        this.profileLocked = profileLocked;
    }

    // -------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrn() {
        return prn;
    }

    public void setPrn(String prn) {
        this.prn = prn;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isProfileLocked() {
        return profileLocked;
    }

    public void setProfileLocked(boolean profileLocked) {
        this.profileLocked = profileLocked;
    }
}
