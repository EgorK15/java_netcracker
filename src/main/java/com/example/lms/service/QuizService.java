package com.example.lms.service;

import com.example.lms.dto.*;
import com.example.lms.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final LessonService lessonService;
    private final EnrollmentService enrollmentService;

    @Transactional
    public QuizResultResponse submitQuiz(Long quizId, SubmitQuizRequest request) {
        Enrollment enrollment = enrollmentService.getForUpdate(request.enrollmentId());
        enrollmentService.requireActive(enrollment);
        QuizLesson quiz = lessonService.findQuiz(quizId);
        enrollmentService.requireCourseLesson(enrollment, quiz);
        Map<Long, Integer> answers = new HashMap<>();
        for (QuizAnswerRequest answer : request.answers()) {
            if (answers.putIfAbsent(answer.questionId(), answer.selectedOption()) != null) {
                throw new IllegalArgumentException("Нельзя ответить на один вопрос дважды");
            }
        }
        if (quiz.getQuestions().isEmpty() || answers.size() != quiz.getQuestions().size()) {
            throw new IllegalArgumentException("Нужен ровно один ответ на каждый вопрос теста");
        }
        int correctCount = 0;
        for (Question question : quiz.getQuestions()) {
            Integer selected = answers.get(question.getId());
            if (selected == null || selected < 0 || selected >= question.getOptions().size()) {
                throw new IllegalArgumentException("Неизвестный вопрос или неверный индекс ответа");
            }
            if (question.isCorrectAnswer(selected)) {
                correctCount++;
            }
        }
        int total = quiz.getQuestions().size();
        int score = correctCount * 100 / total;
        boolean passed = score >= quiz.getPassingScore();
        enrollmentService.saveProgress(enrollment, quiz, score, passed);
        return QuizResultResponse.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .totalQuestions(total)
                .correctAnswers(correctCount)
                .scorePercent(score)
                .passed(passed)
                .passingScore(quiz.getPassingScore())
                .build();
    }
}
