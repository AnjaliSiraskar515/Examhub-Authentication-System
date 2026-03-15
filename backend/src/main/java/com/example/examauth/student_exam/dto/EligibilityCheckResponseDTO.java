package com.example.examauth.student_exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCheckResponseDTO {
    private boolean eligible;
    private String message;
}
