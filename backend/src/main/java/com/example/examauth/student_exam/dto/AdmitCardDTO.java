package com.example.examauth.student_exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdmitCardDTO {
    private String studentName;
    private String seatNumber;
    private String prn;
    private String course;
    private String semester;
    private String examName;
    private String centerName;
    private List<SubjectScheduleDTO> subjects = new ArrayList<>();
    private String qrCode;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectScheduleDTO {
        private String subjectName;
        private String date;
        private String time;
    }
}
