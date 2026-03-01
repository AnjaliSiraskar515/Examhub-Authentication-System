package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ExamName;
    private String subject;
    private LocalDate examDate;
    private String examTime;
    private boolean active;
    private String qrCodeValue;
    

    @Column(nullable = false)
private String status = "Scheduled";

public LocalDate getExamDate() {
    return examDate;
}

public String getExamName() {
    return ExamName;
}

public void setExamName(String examName) {
    this.ExamName = examName;
}

public Exam() {}

public Exam(Long id, LocalDate examDate) {
    this.id = id;
    this.examDate = examDate;
}

public Long getId() {
        return id;
    }


public void setExamDate(LocalDate examDate) {
    this.examDate = examDate;
}

public boolean isActive() {
    return active;
}

public void setActive(boolean active) {
    this.active = active;
}

public String getQrCodeValue() {
    return qrCodeValue;
}

public void setQrCodeValue(String qrCodeValue) {
    this.qrCodeValue = qrCodeValue;
}


    


    // getters setters
}
