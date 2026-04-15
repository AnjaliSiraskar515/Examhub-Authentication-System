package com.example.examauth.student_exam.university.service;

import com.example.examauth.student_exam.university.model.UniversityExam;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UniversityExamService {

    private final UniversityExamRepository repository;

    @Transactional
    public UniversityExam createExam(UniversityExam request) {
        if (request.getSubjects() != null) {
            request.getSubjects().forEach(sub -> sub.setUniversityExam(request));
        }
        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            request.setStatus("DRAFT");
        }
        return repository.save(request);
    }

    @Transactional
    public UniversityExam updateExam(Long id, UniversityExam request) {
        UniversityExam exam = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + id));

        exam.setSessionName(request.getSessionName());
        exam.setAcademicYear(request.getAcademicYear());
        exam.setExamType(request.getExamType());
        exam.setMode(request.getMode());
        exam.setCourse(request.getCourse());
        exam.setDepartment(request.getDepartment());
        exam.setSemester(request.getSemester());
        exam.setRegistrationWindow(request.getRegistrationWindow());
        exam.setSchedule(request.getSchedule());
        exam.setFeeStructure(request.getFeeStructure());
        exam.setControls(request.getControls());
        exam.setStatus(request.getStatus() != null ? request.getStatus() : exam.getStatus());

        exam.setSupervisorId(request.getSupervisorId());
        exam.setSupervisorName(request.getSupervisorName());
        // Preserve institutionCode: only update if provided in request
        if (request.getInstitutionCode() != null && !request.getInstitutionCode().isEmpty()) {
            exam.setInstitutionCode(request.getInstitutionCode());
        }

        exam.setCenterName(request.getCenterName());
        exam.setCenterCode(request.getCenterCode());
        exam.setCenterCapacity(request.getCenterCapacity());
        exam.setReportingTime(request.getReportingTime());
        exam.setPlatformName(request.getPlatformName());
        exam.setExamLink(request.getExamLink());
        exam.setProctoringEnabled(request.getProctoringEnabled());

        if (request.getSubjects() != null) {
            exam.getSubjects().clear();
            request.getSubjects().forEach(exam::addSubject);
        }

        return repository.save(exam);
    }

    @Transactional
    public UniversityExam publishExam(Long id) {
        UniversityExam exam = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + id));
        exam.setStatus("OPEN");
        return repository.save(exam);
    }

    public List<UniversityExam> getAllExams() {
        return repository.findAll();
    }

    public UniversityExam getExamById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + id));
    }

    @Transactional
    public void deleteExam(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Exam not found with ID: " + id);
        }
        repository.deleteById(id);
    }
}
