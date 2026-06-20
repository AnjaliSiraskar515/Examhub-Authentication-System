package com.example.examauth.service;

import com.example.examauth.model.Exam;
import com.example.examauth.repo.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExamService {

    @Autowired
    private ExamRepository examRepository;

    public Exam createExam(Exam exam) {
        return examRepository.save(exam);
    }

    public List<Exam> getExamsByInstitution(String institutionName) {
        return examRepository.findByInstitutionName(institutionName);
    }

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public Optional<Exam> getExamById(Long id) {
        return examRepository.findById(id);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteExam(Long id) {
        // We cannot easily autowire all repositories here without cyclic dependencies, 
        // but we can use EntityManager or the repositories if we inject them.
        examRepository.deleteById(id);
    }
}
