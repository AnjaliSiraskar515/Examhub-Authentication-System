package com.example.examauth.service;

import com.example.examauth.exception.EligibilityException;
import com.example.examauth.model.Exam;
import com.example.examauth.model.ExamType;
import com.example.examauth.model.StudentBacklog;
import com.example.examauth.model.User;
import com.example.examauth.repo.ExamRepository;
import com.example.examauth.repo.StudentBacklogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentExamEligibilityService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private StudentBacklogRepository backlogRepository;

    public List<Exam> getEligibleExams(User student) {
        // Eligibility Gate
        if (Boolean.FALSE.equals(student.getFeesPaid())) {
            throw new EligibilityException("Outstanding fees. Please clear dues.");
        }
        if (Boolean.FALSE.equals(student.getIsEligible())) {
            throw new EligibilityException("Student is marked as ineligible.");
        }
        if (Boolean.FALSE.equals(student.getExamAccessAllowed())) {
            throw new EligibilityException("Exam access is currently revoked. Please contact admin.");
        }

        List<Exam> eligibleExams = new ArrayList<>();

        // Fetch Regular Exams
        List<Exam> regularExams = examRepository.findRegularExams(student.getSemester(), ExamType.REGULAR, "upcoming");
        if (regularExams != null) {
            eligibleExams.addAll(regularExams);
        }

        // Fetch Backlog Exams
        List<StudentBacklog> backlogs = backlogRepository.findByStudentIdAndCleared(student.getUserId(), false);
        if (backlogs != null && !backlogs.isEmpty()) {
            List<Long> subjectIds = backlogs.stream().map(StudentBacklog::getSubjectId).collect(Collectors.toList());
            List<Exam> backlogExams = examRepository.findBacklogExams(subjectIds, ExamType.BACKLOG, "upcoming");
            if (backlogExams != null) {
                eligibleExams.addAll(backlogExams);
            }
        }

        return eligibleExams;
    }
}
