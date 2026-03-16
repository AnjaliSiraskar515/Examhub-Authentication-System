package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long examId;
    private LocalDate examDate;
    private String startTime;
    private String endTime;

public Long getExamId() {
    return examId;
}

public void setExamId(Long examId) {
    this.examId = examId;
}

public LocalDate getExamDate() {
    return examDate;
}

public void setExamDate(LocalDate examDate) {
    this.examDate = examDate;
}


    // getters setters
}
