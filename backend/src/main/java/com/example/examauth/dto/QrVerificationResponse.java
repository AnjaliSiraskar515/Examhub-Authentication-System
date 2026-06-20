package com.example.examauth.dto;

public class QrVerificationResponse {

    private boolean valid;
    private String message;

    // Biometric summary flags for UI flow
    private boolean biometricEnrolled;
    private boolean biometricRecentlyVerified;

    private StudentDetail student;
    private ExamDetail exam;
    private InstitutionDetail institution;

    public QrVerificationResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    // Getters and Setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isBiometricEnrolled() {
        return biometricEnrolled;
    }

    public void setBiometricEnrolled(boolean biometricEnrolled) {
        this.biometricEnrolled = biometricEnrolled;
    }

    public boolean isBiometricRecentlyVerified() {
        return biometricRecentlyVerified;
    }

    public void setBiometricRecentlyVerified(boolean biometricRecentlyVerified) {
        this.biometricRecentlyVerified = biometricRecentlyVerified;
    }

    public StudentDetail getStudent() {
        return student;
    }

    public void setStudent(StudentDetail student) {
        this.student = student;
    }

    public ExamDetail getExam() {
        return exam;
    }

    public void setExam(ExamDetail exam) {
        this.exam = exam;
    }

    public InstitutionDetail getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionDetail institution) {
        this.institution = institution;
    }

    // Nested DTOs
    public static class StudentDetail {
        private Long id;
        private String name;
        private String rollNo;
        private String prn;
        private String photoUrl;
        private String aadharMasked;
        private String hallNo;
        private String seatNo;
        private String courseInfo;

        public StudentDetail(String name, String rollNo, String photoUrl, String aadharMasked) {
            this.id = null;
            this.name = name;
            this.rollNo = rollNo;
            this.prn = null;
            this.photoUrl = photoUrl;
            this.aadharMasked = aadharMasked;
            this.hallNo = null;
            this.seatNo = null;
        }

        // Extended constructor with richer details (preferred for new flows)
        public StudentDetail(Long id, String name, String rollNo, String prn,
                             String photoUrl, String aadharMasked,
                             String hallNo, String seatNo) {
            this.id = id;
            this.name = name;
            this.rollNo = rollNo;
            this.prn = prn;
            this.photoUrl = photoUrl;
            this.aadharMasked = aadharMasked;
            this.hallNo = hallNo;
            this.seatNo = seatNo;
        }

        // Getters
        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getRollNo() {
            return rollNo;
        }

        public String getPrn() {
            return prn;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public String getAadharMasked() {
            return aadharMasked;
        }

        public String getHallNo() {
            return hallNo;
        }

        public String getSeatNo() {
            return seatNo;
        }

        public String getCourseInfo() {
            return courseInfo;
        }

        public void setCourseInfo(String courseInfo) {
            this.courseInfo = courseInfo;
        }
    }

    public static class ExamDetail {
        private Long examId;
        private Long subjectId;
        private String examName;
        private String subject;
        private String subjectCode;
        private String date;
        private String time;

        public ExamDetail(Long examId, Long subjectId, String examName, String subject, String subjectCode, String date, String time) {
            this.examId = examId;
            this.subjectId = subjectId;
            this.examName = examName;
            this.subject = subject;
            this.subjectCode = subjectCode;
            this.date = date;
            this.time = time;
        }

        // Getters
        public String getExamName() {
            return examName;
        }

        public Long getExamId() {
            return examId;
        }

        public Long getSubjectId() {
            return subjectId;
        }

        public String getSubject() {
            return subject;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getDate() {
            return date;
        }

        public String getTime() {
            return time;
        }
    }

    public static class InstitutionDetail {
        private String name;
        private String centerCode;

        public InstitutionDetail(String name, String centerCode) {
            this.name = name;
            this.centerCode = centerCode;
        }

        // Getters
        public String getName() {
            return name;
        }

        public String getCenterCode() {
            return centerCode;
        }
    }
}
