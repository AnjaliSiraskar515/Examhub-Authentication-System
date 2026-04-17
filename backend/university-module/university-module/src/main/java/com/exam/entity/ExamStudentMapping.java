package com.exam.entity;

import jakarta.persistence.*;

@Entity
public class ExamStudentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long examId;
    private Long studentId;

    // getters setters
}

