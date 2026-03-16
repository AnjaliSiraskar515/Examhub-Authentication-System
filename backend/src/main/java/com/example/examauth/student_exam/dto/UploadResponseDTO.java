package com.example.examauth.student_exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponseDTO {
    private int totalRecords;
    private int insertedRecords;
    private int skippedDuplicates;
    private boolean success;
    private String message;
}
