package com.example.examauth.student_exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatsDTO {
    private int totalRegistered;
    private int pendingVerifications;
    private int approvedExams;
    private int upcomingExams;
}
