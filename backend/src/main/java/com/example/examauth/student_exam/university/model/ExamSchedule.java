package com.example.examauth.student_exam.university.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Embeddable
@Data
public class ExamSchedule {
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
