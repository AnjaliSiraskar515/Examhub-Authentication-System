package com.exam.entity;

import jakarta.persistence.*;
@Entity
public class College {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private String CollegeName;
    private String address;

    public String getCollegeName() {
        return CollegeName;
    }

    public void setCollegeName(String collegeName) {
        this.CollegeName = collegeName;
    }

    // getters and setters
}
