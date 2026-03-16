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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    @JsonIgnore
    private UniversityExam universityExam;
}
