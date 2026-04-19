package com.example.examauth.controller;

import com.example.examauth.model.StudentBacklog;
import com.example.examauth.model.Subject;
import com.example.examauth.model.User;
import com.example.examauth.repo.StudentBacklogRepository;
import com.example.examauth.repo.SubjectRepository;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.service.CsvProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/backlogs")
public class AdminBacklogController {

    @Autowired
    private CsvProcessingService csvProcessingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private StudentBacklogRepository studentBacklogRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadBacklogCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a CSV file!");
        }

        try {
            List<String[]> rows = csvProcessingService.parseCsv(file);
            int processedCount = 0;
            for (String[] row : rows) {
                if (row.length < 2) continue;
                String prn = row[0];
                String subjectCode = row[1];
                
                Optional<User> userOpt = userRepository.findByPrn(prn);
                Optional<Subject> subjectOpt = subjectRepository.findByCode(subjectCode);
                
                if (userOpt.isPresent() && subjectOpt.isPresent()) {
                    Long studentId = userOpt.get().getUserId();
                    Long subjectId = subjectOpt.get().getId();
                    
                    if (studentBacklogRepository.findByStudentIdAndSubjectId(studentId, subjectId).isEmpty()) {
                        StudentBacklog backlog = new StudentBacklog();
                        backlog.setStudentId(studentId);
                        backlog.setSubjectId(subjectId);
                        backlog.setCleared(false);
                        studentBacklogRepository.save(backlog);
                        processedCount++;
                    }
                }
            }
            return ResponseEntity.ok("Processed " + processedCount + " backlog mapping records successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process CSV file: " + e.getMessage());
        }
    }
}
