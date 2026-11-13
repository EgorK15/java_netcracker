package com.example.lms.dto;

import lombok.Builder;
@Builder
public record QuizResultResponse(Long quizId, String quizTitle, int totalQuestions,
                                 int correctAnswers, int scorePercent, boolean passed, int passingScore) {
}
