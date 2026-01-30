package com.example.examauth.student_exam.service;

import com.example.examauth.student_exam.dto.ExamResponseDTO;
import com.example.examauth.student_exam.model.Exam;
import com.example.examauth.student_exam.repo.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;

    public List<ExamResponseDTO> getAllExams() {
        List<Exam> exams = examRepository.findAll();
        return exams.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private ExamResponseDTO mapToResponseDTO(Exam exam) {
        ExamResponseDTO dto = new ExamResponseDTO();
        dto.setId(exam.getId());
        dto.setExamName(exam.getExamName());
        dto.setExamType(exam.getExamType().name());
        dto.setExamDate(exam.getExamDate());
        dto.setStatus(exam.getStatus());
        return dto;
    }

    // TODO: Implement method to get exam by ID

    // TODO: Implement method to get exams by type (UNIVERSITY/COLLEGE)

    // TODO: Implement method to get upcoming exams

    // TODO: Implement method to search exams
}
