package com.example.examauth.student_exam.university.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class ExamControls {
    private Integer maxStudents;
    private Boolean requireFaceVerification;
    private Boolean allowEditAfterPublish;
}
