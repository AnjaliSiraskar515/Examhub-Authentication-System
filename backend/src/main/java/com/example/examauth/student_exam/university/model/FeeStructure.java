package com.example.examauth.student_exam.university.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.math.BigDecimal;

@Embeddable
@Data
public class FeeStructure {
    private BigDecimal regularFee;
    private BigDecimal backlogFee;
    private BigDecimal lateFee;
}
