package com.example.examauth.dto;

import java.util.List;
import java.util.Map;

public class AnalyticsDTO {
    private List<String> labels;
    private List<Integer> examCounts;
    private List<Integer> studentCounts;
    private List<Integer> avgScores;
    private List<Integer> participants;

    public AnalyticsDTO(List<String> labels, List<Integer> examCounts, List<Integer> studentCounts, List<Integer> avgScores, List<Integer> participants) {
        this.labels = labels;
        this.examCounts = examCounts;
        this.studentCounts = studentCounts;
        this.avgScores = avgScores;
        this.participants = participants;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<Integer> getExamCounts() {
        return examCounts;
    }

    public void setExamCounts(List<Integer> examCounts) {
        this.examCounts = examCounts;
    }

    public List<Integer> getStudentCounts() {
        return studentCounts;
    }

    public void setStudentCounts(List<Integer> studentCounts) {
        this.studentCounts = studentCounts;
    }

    public List<Integer> getAvgScores() {
        return avgScores;
    }

    public void setAvgScores(List<Integer> avgScores) {
        this.avgScores = avgScores;
    }

    public List<Integer> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Integer> participants) {
        this.participants = participants;
    }
}
