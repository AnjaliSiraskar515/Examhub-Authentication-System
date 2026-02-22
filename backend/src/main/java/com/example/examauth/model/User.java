package com.example.examauth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;
    private String username; // Added username field for Supervisor/Auth controllers
    private String email;

    // ✅ New field added here
    @Column(unique = true, nullable = true, length = 20)
    private String prn; // For student login via PRN

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

    // Exam Verification Status
    private Boolean qrVerified = false;
    private Boolean biometricVerified = false;

    public Boolean getQrVerified() {
        return qrVerified;
    }

    public void setQrVerified(Boolean qrVerified) {
        this.qrVerified = qrVerified;
    }

    public Boolean getBiometricVerified() {
        return biometricVerified;
    }

    public void setBiometricVerified(Boolean biometricVerified) {
        this.biometricVerified = biometricVerified;
    }

    private java.time.LocalDateTime lastLogin;

    public java.time.LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(java.time.LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    // Academic Info
    private String department;
    private String major;
    private String year;
    private String semester;
    private String enrollmentNo;
    private String cgpa;

    // Personal Info
    private String dob;
    private String gender;

    // ===========================================================
    // Supervisor Profile Fields (Added)
    // ===========================================================
    private String universityName;
    private String collegeName;
    private String designation; // e.g. Chief Supervisor, Room Invigilator
    private String employeeId;
    private String appointmentLetterPath;

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getAppointmentLetterPath() {
        return appointmentLetterPath;
    }

    public void setAppointmentLetterPath(String appointmentLetterPath) {
        this.appointmentLetterPath = appointmentLetterPath;
    }

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // ✅ New getter/setter for PRN
    public String getPrn() {
        return prn;
    }

    public void setPrn(String prn) {
        this.prn = prn;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getEnrollmentNo() {
        return enrollmentNo;
    }

    public void setEnrollmentNo(String enrollmentNo) {
        this.enrollmentNo = enrollmentNo;
    }

    public String getCgpa() {
        return cgpa;
    }

    public void setCgpa(String cgpa) {
        this.cgpa = cgpa;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
