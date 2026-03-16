package com.example.examauth.student_exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamRegistrationResponseDTO {

    private Long id;
    private Long studentId;
    private Long examId;
    private String prn;
    private String fullName;
    private String course;
    private String examSession;
    private List<String> selectedSubjects;
    private Double totalFee;
    private String paymentStatus;
    private String examType;
    private String registrationStatus;
    private String appliedDate;
}
