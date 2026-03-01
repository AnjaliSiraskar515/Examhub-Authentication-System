package com.exam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.exam")
public class UniversityModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniversityModuleApplication.class, args);
    }

    
}
