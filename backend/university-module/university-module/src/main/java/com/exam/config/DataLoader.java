package com.exam.config;

import com.exam.entity.Exam;
import com.exam.repository.ExamRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataLoader {

    private final ExamRepository examRepository;

    public DataLoader(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @PostConstruct
    public void loadData() {

        if (examRepository.count() == 0) {

            examRepository.save(new Exam(null, LocalDate.of(2026, 3, 10)));
            examRepository.save(new Exam(null, LocalDate.of(2026, 3, 15)));
            examRepository.save(new Exam(null, LocalDate.of(2026, 3, 20)));
            examRepository.save(new Exam(null, LocalDate.of(2026, 4, 1)));
            examRepository.save(new Exam(null, LocalDate.of(2026, 4, 10)));
            examRepository.save(new Exam(null, LocalDate.of(2026, 4, 20)));
            examRepository.save(new Exam(null, LocalDate.of(2026, 5, 5)));
            examRepository.save(new Exam(null, LocalDate.of(2026, 5, 15)));

            System.out.println("✅ Sample exams inserted.");
        }
    }
}
    

