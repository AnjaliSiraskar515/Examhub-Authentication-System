package com.example.examauth.student_exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamRegistrationRequestDTO {

    private Long studentId;

    @NotNull(message = "Exam ID is required")
    private Long examId;

    @NotBlank(message = "PRN is required")
    private String prn;

    @NotBlank(message = "Full Name is required")
    private String fullName;

    @NotBlank(message = "Course is required")
    private String course;

    @NotBlank(message = "Year is required")
    private String year;

    private String examType;
    private String institutionName;
    private String examSession;

    // New Fields for Professional Registration
    private List<String> selectedSubjects;
    private Boolean declarationAccepted;
    private String paymentStatus;
    private Double totalFee;

    /**
     * Compatibility constructor for legacy code
     */
    public ExamRegistrationRequestDTO(Long studentId, Long examId, String prn, String fullName,
            String course, String year, String examType,
            String institutionName, String examSession) {
        this.studentId = studentId;
        this.examId = examId;
        this.prn = prn;
        this.fullName = fullName;
        this.course = course;
        this.year = year;
        this.examType = examType;
        this.institutionName = institutionName;
        this.examSession = examSession;
    }
}
