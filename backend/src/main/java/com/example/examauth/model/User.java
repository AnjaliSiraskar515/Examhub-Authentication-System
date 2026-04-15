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

    // Used for JWT invalidation: increment this to force logout on all existing tokens
    @Column(columnDefinition = "INT DEFAULT 0")
    private int tokenVersion = 0;

    @Column(length = 2000)
    private String biometricHash;

    // Biometric module fields
    private boolean biometricEnrolled;

    @Column(length = 256)
    private String biometricTemplateHash;

    private java.time.LocalDateTime biometricEnrolledAt;
    private java.time.LocalDateTime biometricLastVerified;

    private String photoPath;

    // Document paths for profile completion
    private String aadharPath;
    private String marks10Path;
    private String marks12Path;
    private String sem1MarksheetPath;
    private String sem2MarksheetPath;
    private String sem3MarksheetPath;
    private String sem4MarksheetPath;
    private String sem5MarksheetPath;
    private String sem6MarksheetPath;
    private String sem7MarksheetPath;
    private String sem8MarksheetPath;

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

    public boolean isBiometricEnrolled() {
        return biometricEnrolled;
    }

    public void setBiometricEnrolled(boolean biometricEnrolled) {
        this.biometricEnrolled = biometricEnrolled;
    }

    public String getBiometricTemplateHash() {
        return biometricTemplateHash;
    }

    public void setBiometricTemplateHash(String biometricTemplateHash) {
        this.biometricTemplateHash = biometricTemplateHash;
    }

    public java.time.LocalDateTime getBiometricEnrolledAt() {
        return biometricEnrolledAt;
    }

    public void setBiometricEnrolledAt(java.time.LocalDateTime biometricEnrolledAt) {
        this.biometricEnrolledAt = biometricEnrolledAt;
    }

    public java.time.LocalDateTime getBiometricLastVerified() {
        return biometricLastVerified;
    }

    public void setBiometricLastVerified(java.time.LocalDateTime biometricLastVerified) {
        this.biometricLastVerified = biometricLastVerified;
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
    private String institutionCode; // To uniquely link to Institution record
    private String universityLogoPath; // filename stored in uploads/logo/
    private String designation; // e.g. Chief Supervisor, Room Invigilator
    private String employeeId;
    private String appointmentLetterPath;

    public String getUniversityLogoPath() {
        return universityLogoPath;
    }

    public void setUniversityLogoPath(String universityLogoPath) {
        this.universityLogoPath = universityLogoPath;
    }


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

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
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

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
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

    public String getSem1MarksheetPath() {
        return sem1MarksheetPath;
    }

    public void setSem1MarksheetPath(String sem1MarksheetPath) {
        this.sem1MarksheetPath = sem1MarksheetPath;
    }

    public String getSem2MarksheetPath() {
        return sem2MarksheetPath;
    }

    public void setSem2MarksheetPath(String sem2MarksheetPath) {
        this.sem2MarksheetPath = sem2MarksheetPath;
    }

    public String getSem3MarksheetPath() {
        return sem3MarksheetPath;
    }

    public void setSem3MarksheetPath(String sem3MarksheetPath) {
        this.sem3MarksheetPath = sem3MarksheetPath;
    }

    public String getSem4MarksheetPath() {
        return sem4MarksheetPath;
    }

    public void setSem4MarksheetPath(String sem4MarksheetPath) {
        this.sem4MarksheetPath = sem4MarksheetPath;
    }

    public String getSem5MarksheetPath() {
        return sem5MarksheetPath;
    }

    public void setSem5MarksheetPath(String sem5MarksheetPath) {
        this.sem5MarksheetPath = sem5MarksheetPath;
    }

    public String getSem6MarksheetPath() {
        return sem6MarksheetPath;
    }

    public void setSem6MarksheetPath(String sem6MarksheetPath) {
        this.sem6MarksheetPath = sem6MarksheetPath;
    }

    public String getSem7MarksheetPath() {
        return sem7MarksheetPath;
    }

    public void setSem7MarksheetPath(String sem7MarksheetPath) {
        this.sem7MarksheetPath = sem7MarksheetPath;
    }

    public String getSem8MarksheetPath() {
        return sem8MarksheetPath;
    }

    public void setSem8MarksheetPath(String sem8MarksheetPath) {
        this.sem8MarksheetPath = sem8MarksheetPath;
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
