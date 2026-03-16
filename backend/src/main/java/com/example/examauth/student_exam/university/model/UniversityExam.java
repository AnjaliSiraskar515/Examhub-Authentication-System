package com.example.examauth.student_exam.university.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "new_university_exams")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UniversityExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionName;
    private String academicYear;
    private String examType;
    private String mode;
    private String course;
    private String department;
    private String semester;
    private String status;

    @OneToMany(mappedBy = "universityExam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UniversityExamSubject> subjects;

    @Embedded
    private RegistrationWindow registrationWindow;

    @Embedded
    private ExamSchedule schedule;

    @Embedded
    private FeeStructure feeStructure;

    @Embedded
    private ExamControls controls;

    // Optional Mode Specific configurations can be flattened to avoid overly deep
    // structure
    private String centerName;
    private String centerCode;
    private Integer centerCapacity;
    private String reportingTime;
    private String platformName;
    private String examLink;
    private Boolean proctoringEnabled;

    // Backward compatibility helper methods for old services
    public String getExamName() {
        return sessionName;
    }

    public LocalDate getExamDate() {
        if (schedule != null) {
            return schedule.getExamDate();
        }
        return null;
    }

    public void addSubject(UniversityExamSubject subject) {
        subjects.add(subject);
        subject.setUniversityExam(this);
    }
}
