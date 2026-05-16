package com.exam.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.exam.entity.College;
import com.exam.repository.CollegeRepository;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
@RestController
@RequestMapping("/api/colleges")
@CrossOrigin(origins = "*")
public class CollegeController {

    @Autowired
    private CollegeRepository repository;

    @GetMapping
    public List<College> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public College add(@RequestBody College college) {
        return repository.save(college);
    }

    
}
