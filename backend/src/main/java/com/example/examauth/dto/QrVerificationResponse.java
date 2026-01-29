package com.example.examauth.dto;

public class QrVerificationResponse {

    private boolean valid;
    private String message;

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
        private String name;
        private String rollNo;
        private String photoUrl;
        private String aadharMasked;

        public StudentDetail(String name, String rollNo, String photoUrl, String aadharMasked) {
            this.name = name;
            this.rollNo = rollNo;
            this.photoUrl = photoUrl;
            this.aadharMasked = aadharMasked;
        }

        // Getters
        public String getName() {
            return name;
        }

        public String getRollNo() {
            return rollNo;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public String getAadharMasked() {
            return aadharMasked;
        }
    }

    public static class ExamDetail {
        private String examName;
        private String subject;
        private String date;
        private String time;

        public ExamDetail(String examName, String subject, String date, String time) {
            this.examName = examName;
            this.subject = subject;
            this.date = date;
            this.time = time;
        }

        // Getters
        public String getExamName() {
            return examName;
        }

        public String getSubject() {
            return subject;
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
