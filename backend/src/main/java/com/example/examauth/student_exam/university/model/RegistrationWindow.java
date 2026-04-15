package com.example.examauth.student_exam.university.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.time.LocalDate;

@Embeddable
@Data
public class RegistrationWindow {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate lateFeeDeadline;
}
