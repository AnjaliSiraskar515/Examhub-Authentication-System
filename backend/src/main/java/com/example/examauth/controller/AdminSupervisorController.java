package com.example.examauth.controller;

import com.example.examauth.service.CsvProcessingService;
import com.example.examauth.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;



@RestController
@RequestMapping("/api/admin/supervisors")
public class AdminSupervisorController {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private CsvProcessingService csvProcessingService;

    @PostMapping
    public ResponseEntity<?> createSupervisor(@RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("name");
            String email = (String) payload.get("email");
            String phone = (String) payload.get("phone");
            String designation = (String) payload.get("designation");
            String department = (String) payload.get("department");
            Object collegeIdObj = payload.get("collegeId");
            Long collegeId = collegeIdObj != null ? Long.valueOf(collegeIdObj.toString()) : null;
            
            userManagementService.createSupervisor(name, email, phone, designation, department, collegeId);
            return ResponseEntity.ok("Supervisor created successfully. Credentials emailed.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSupervisorCsv(@RequestParam("file") MultipartFile file, @RequestParam(value = "collegeId", required = false) Long collegeId) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a CSV file!");
        }

        try {
            List<String[]> rows = csvProcessingService.parseCsv(file);
            int processedCount = 0;
            for (String[] row : rows) {
                if (row.length < 4) continue;
                String name = row[0];
                String email = row[1];
                String phone = row[2];
                String designation = row[3];
                String department = row.length > 4 ? row[4] : null;
                
                try {
                    userManagementService.createSupervisor(name, email, phone, designation, department, collegeId);
                    processedCount++;
                } catch (Exception ignored) {
                    // Skip existing supervisors
                }
            }
            return ResponseEntity.ok("Processed " + processedCount + " supervisor records successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process CSV file: " + e.getMessage());
        }
    }
}
