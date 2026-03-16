package com.example.examauth.student_exam.university;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDTO {
    private long totalStudents; // Mocked or fetched from Auth service if possible, else 0
    private long activeExams;
    private long totalRegistrations;
    private long approvedRegistrations;
    private long pendingApprovals;
}
