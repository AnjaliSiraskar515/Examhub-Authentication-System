
package com.example.examauth.service;

import com.example.examauth.exception.EligibilityException;
import com.example.examauth.model.Exam;
import com.example.examauth.model.ExamType;
import com.example.examauth.model.StudentBacklog;
import com.example.examauth.model.User;
import com.example.examauth.repo.ExamRepository;
import com.example.examauth.repo.StudentBacklogRepository;
import com.example.examauth.repo.SubjectRepository;
import com.example.examauth.model.Subject;
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

    @Autowired
    private SubjectRepository subjectRepository;

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

        // Check if student is BACKLOG type or has uncleared backlogs
        List<StudentBacklog> backlogs = backlogRepository.findByStudentIdAndCleared(student.getUserId(), false);
        boolean hasActiveBacklogs = (backlogs != null && !backlogs.isEmpty())
                || "BACKLOG".equals(student.getStudentType());

        if (hasActiveBacklogs && backlogs != null && !backlogs.isEmpty()) {
            // Fetch Backlog Exams ONLY
            List<Long> subjectIds = backlogs.stream()
                    .map(StudentBacklog::getSubjectId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());

            if (!subjectIds.isEmpty()) {
                List<Exam> backlogExams = examRepository.findBacklogExams(subjectIds, ExamType.BACKLOG, "upcoming");
                if (backlogExams != null) {
                    // Handle safely: Filter out old exams that might have subjectId = null
                    eligibleExams.addAll(backlogExams.stream()
                            .filter(exam -> exam.getSubjectId() != null)
                            .collect(Collectors.toList()));
                }
            }
        } else {
            // Treat as REGULAR (Fallback if they have studentType = BACKLOG but no actual backlogs)
            List<Long> strictSubjectIds = new ArrayList<>();
            if (student.getCollege() != null && student.getCourse() != null && student.getSemester() != null) {
                try {
                    int sem = Integer.parseInt(student.getSemester());
                    List<Subject> subjects = subjectRepository.findByCourseSemesterAndCollege(student.getCourse(), sem, student.getCollege().getId());
                    strictSubjectIds = subjects.stream().map(Subject::getId).collect(Collectors.toList());
                } catch (Exception e) {
                    System.err.println("Failed parsing semester for strict mapping: " + e.getMessage());
                }
            }

            if (!strictSubjectIds.isEmpty()) {
                // Future Optimization Note: If strictSubjectIds.size() > 100+, rewrite this query
                // with JOIN subject s ON exam.subject_id = s.id WHERE s.course = ? etc.
                List<Exam> regularExams = examRepository.findRegularExamsBySubjectIds(strictSubjectIds, ExamType.REGULAR, "upcoming");
                if (regularExams != null) {
                    eligibleExams.addAll(regularExams.stream()
                            .filter(exam -> exam.getSubjectId() != null)
                            .collect(Collectors.toList()));
                }
            } else {
                // Fallback for students with incomplete profiles
                List<Exam> fallbackExams = examRepository.findRegularExams(student.getSemester(), ExamType.REGULAR, "upcoming");
                if (fallbackExams != null) {
                    eligibleExams.addAll(fallbackExams.stream()
                            .filter(exam -> exam.getSubjectId() != null)
                            .collect(Collectors.toList()));
                }
            }
        }

        return eligibleExams;
    }
}
