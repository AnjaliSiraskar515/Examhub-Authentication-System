package com.example.examauth.student_exam.university.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "university_exam_subjects")
@Data
public class UniversityExamSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subjectName;
    private String subjectCode;
    private String paperCode;
    private Integer totalMarks;
    private Integer passingMarks;
    private Integer duration;

    // Scheduling per subject
    private java.time.LocalDate examDate;
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    @JsonIgnore
    private UniversityExam universityExam;
}
