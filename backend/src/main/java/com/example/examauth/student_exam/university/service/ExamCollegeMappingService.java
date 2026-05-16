package com.example.examauth.student_exam.university.service;

import com.example.examauth.student_exam.university.model.ExamCollegeMapping;
import com.example.examauth.student_exam.university.repo.ExamCollegeMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamCollegeMappingService {

    private final ExamCollegeMappingRepository mappingRepository;

    @Transactional
    public ExamCollegeMapping createMapping(ExamCollegeMapping mapping) {
        if (mappingRepository.existsByExamIdAndCollegeId(mapping.getExamId(), mapping.getCollegeId())) {
            throw new IllegalStateException("Mapping already exists for this exam and college");
        }
        return mappingRepository.save(mapping);
    }

    public List<ExamCollegeMapping> getMappingsByExam(Long examId) {
        return mappingRepository.findAllByExamId(examId);
    }

    public List<ExamCollegeMapping> getMappingsBySupervisor(Long supervisorId) {
        return mappingRepository.findAllByHeadSupervisorId(supervisorId);
    }

    public List<ExamCollegeMapping> getMappingsByCollegeId(Long collegeId) {
        return mappingRepository.findAllByCollegeId(collegeId);
    }

    public ExamCollegeMapping getMapping(Long examId, Long collegeId) {
        return mappingRepository.findByExamIdAndCollegeId(examId, collegeId)
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found"));
    }
}
