package com.example.examauth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "departments", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "college_id" })
})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private College college;

    public Department() {
    }

    public Department(String name, College college) {
        this.name = name;
        this.college = college;
    }

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

    public College getCollege() {
        return college;
    }

    public void setCollege(College college) {
        this.college = college;
    }
}
