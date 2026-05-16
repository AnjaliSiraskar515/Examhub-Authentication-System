package com.example.examauth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames={"code", "semester", "department_id", "course"})
    },
    indexes = {
        @Index(name = "idx_subject_code", columnList = "code")
    }
)
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private Integer semester;

    @Column(nullable = false)
    private String course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Department departmentEntity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public Department getDepartmentEntity() {
        return departmentEntity;
    }

    public void setDepartmentEntity(Department departmentEntity) {
        this.departmentEntity = departmentEntity;
    }
}
