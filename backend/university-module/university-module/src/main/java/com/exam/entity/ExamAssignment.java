package com.exam.entity;

import jakarta.persistence.*;

@Entity
public class ExamAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "college_id")
    private College college;

    @ManyToOne
    @JoinColumn(name = "supervisor_id")
    private Supervisor supervisor;

    @Column(nullable = false)
    private String status = "Assigned";

    // GETTERS AND SETTERS
    
    public String getStatus() {
    return status;
}

public void setStatus(String status) {
    this.status = status;
}
    public Long getId() {
        return id;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public Exam getExam() {
        return exam;
    }

    public void setCollege(College college) {
        this.college = college;
    }

    public College getCollege() {
        return college;
    }

    public void setSupervisor(Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    public Supervisor getSupervisor() {
        return supervisor;
    }
}