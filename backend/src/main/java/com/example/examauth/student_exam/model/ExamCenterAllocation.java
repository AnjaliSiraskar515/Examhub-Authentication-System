package com.example.examauth.student_exam.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exam_center_allocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamCenterAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long examId;

    @Column(nullable = false)
    private String centerName;

    @Column(nullable = false)
    private String roomNumber;

    @Column(nullable = false)
    private String seatNumber;
}
