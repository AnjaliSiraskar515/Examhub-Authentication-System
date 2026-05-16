package com.example.examauth.student_exam.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatAllocationDTO {
    private Long id;
    private Long registrationId;
    private String prn;
    private String studentName;
    private String hallName;
    private String seatNumber;
    private String rollNumber;
}
