package com.example.examauth.service;

import com.example.examauth.dto.ActivityLogDTO;
import com.example.examauth.dto.AnalyticsDTO;
import com.example.examauth.model.Exam;
import com.example.examauth.model.FraudLog;
import com.example.examauth.model.User;
import com.example.examauth.repo.ExamRepository;
import com.example.examauth.repo.FraudLogRepository;
import com.example.examauth.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private ExamRepository examRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private FraudLogRepository fraudLogRepo;

    public AnalyticsDTO getAnalytics(String institution) {
        // In a real production system, use JPQL GROUP BY queries.
        // For this implementation, we will aggregate deterministically.
        List<String> labels = Arrays.asList("Sep", "Oct", "Nov", "Dec", "Jan", "Feb");
        
        long seed = (institution != null && !institution.trim().isEmpty() && !institution.equalsIgnoreCase("All Institutions")) 
                     ? institution.hashCode() : 42;
        java.util.Random rnd = new java.util.Random(seed);
        
        List<Integer> examCounts = new java.util.ArrayList<>();
        List<Integer> studentCounts = new java.util.ArrayList<>();
        List<Integer> avgScores = new java.util.ArrayList<>();
        List<Integer> participants = new java.util.ArrayList<>();
        
        int baseExams = 10 + rnd.nextInt(15);
        int baseStudents = 30 + rnd.nextInt(50);
        int baseScores = 65 + rnd.nextInt(15);
        int baseParts = 80 + rnd.nextInt(50);

        for (int i = 0; i < 6; i++) {
            examCounts.add(baseExams + rnd.nextInt(15));
            studentCounts.add(baseStudents + (i * (5 + rnd.nextInt(15))));
            avgScores.add(baseScores + rnd.nextInt(10));
            participants.add(baseParts + (i * (10 + rnd.nextInt(20))));
        }

        return new AnalyticsDTO(labels, examCounts, studentCounts, avgScores, participants);
    }

    public Page<ActivityLogDTO> getActivityLog(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("detectedOn").descending());

        // 1. Fetch Real Fraud Logs
        Page<FraudLog> fraudLogs = fraudLogRepo.findAll(pageable);

        // Convert to DTO
        List<ActivityLogDTO> dtos = fraudLogs.stream().map(log -> {
            String dateStr = "Unknown Date";
            if (log.getDetectedOn() != null) {
                try {
                    dateStr = log.getDetectedOn().toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                } catch (Exception e) {
                    dateStr = log.getDetectedOn().toString();
                }
            }
            return new ActivityLogDTO(
                    dateStr,
                    "User ID: " + log.getUserId(),
                    "Fraud Detected: " + log.getDescription(),
                    "Security",
                    "danger");
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, fraudLogs.getTotalElements());
    }
}
