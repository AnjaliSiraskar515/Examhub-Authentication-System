package com.example.examauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponseDTO {
    private String sourceId; // e.g. LEGACY_4 or UNIV_1001
    private Long id;
    private String source; // LEGACY or UNIVERSITY
    private String examName;
    private LocalDate date;
    private LocalTime startTime;
    private Integer durationMinutes;
    private String status;
    private String mode;
    private String location;
}
