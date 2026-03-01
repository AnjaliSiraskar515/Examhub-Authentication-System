package com.exam.entity;


import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
@Entity
@Getter
@Setter
public class Supervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;

    @Column(nullable = false)
    private String availability = "Available"; // Available / Assigned

    // getters and setters

    public String getAvailability() {
    return availability;
}

public void setAvailability(String availability) {
    this.availability = availability;
}
}
