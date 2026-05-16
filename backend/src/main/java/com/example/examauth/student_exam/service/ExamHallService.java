package com.example.examauth.student_exam.service;

import com.example.examauth.student_exam.model.ExamHall;
import com.example.examauth.student_exam.repo.ExamHallRepository;
import com.example.examauth.student_exam.university.model.ExamCollegeMapping;
import com.example.examauth.student_exam.university.repo.ExamCollegeMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamHallService {

    private final ExamHallRepository examHallRepository;
    private final ExamCollegeMappingRepository mappingRepository;

    @Transactional
    public ExamHall createHall(ExamHall hall) {
        // Enforce uniqueness of hallPrefix per exam and college
        if (examHallRepository.findByExamIdAndCollegeIdAndHallPrefix(hall.getExamId(), hall.getCollegeId(), hall.getHallPrefix()).isPresent()) {
            throw new IllegalStateException("Hall prefix already exists for this exam and college");
        }
        
        // Enforce uniqueness of hallName per exam and college
        if (examHallRepository.findByExamIdAndCollegeIdAndHallName(hall.getExamId(), hall.getCollegeId(), hall.getHallName()).isPresent()) {
            throw new IllegalStateException("Hall name already exists for this exam and college");
        }

        ExamHall savedHall = examHallRepository.save(hall);

        // Update Mapping status and capacity
        ExamCollegeMapping mapping = mappingRepository.findByExamIdAndCollegeId(hall.getExamId(), hall.getCollegeId())
                .orElseThrow(() -> new IllegalArgumentException("Exam mapping not found"));
        
        Integer totalCapacity = examHallRepository.sumCapacityByExamIdAndCollegeId(hall.getExamId(), hall.getCollegeId());
        mapping.setTotalCapacity(totalCapacity);
        if (mapping.getStatus() == ExamCollegeMapping.MappingStatus.PENDING) {
            mapping.setStatus(ExamCollegeMapping.MappingStatus.HALLS_CONFIGURED);
        }
        mappingRepository.save(mapping);

        return savedHall;
    }

    public List<ExamHall> getHallsByExamAndCollege(Long examId, Long collegeId) {
        return examHallRepository.findAllByExamIdAndCollegeId(examId, collegeId);
    }

    public java.util.Optional<ExamHall> getHallById(Long id) {
        return examHallRepository.findById(id);
    }

    @Transactional
    public void deleteHall(Long id) {
        ExamHall hall = examHallRepository.findById(id).orElse(null);
        if (hall != null) {
            Long examId = hall.getExamId();
            Long collegeId = hall.getCollegeId();
            examHallRepository.deleteById(id);
            
            // Recalculate capacity
            ExamCollegeMapping mapping = mappingRepository.findByExamIdAndCollegeId(examId, collegeId).orElse(null);
            if (mapping != null) {
                Integer totalCapacity = examHallRepository.sumCapacityByExamIdAndCollegeId(examId, collegeId);
                mapping.setTotalCapacity(totalCapacity != null ? totalCapacity : 0);
                mappingRepository.save(mapping);
            }
        }
    }
}
