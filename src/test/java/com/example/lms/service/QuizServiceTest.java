package com.example.lms.service;

import com.example.lms.dto.*;
import com.example.lms.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock LessonService lessonService;
    @Mock EnrollmentService enrollmentService;
    @InjectMocks QuizService service;
    QuizLesson quiz;
    Enrollment enrollment;

    @BeforeEach
    void setUp() {
        enrollment = new Enrollment();
        quiz = new QuizLesson();
        quiz.setId(5L);
        quiz.setTitle("Основы Java");
        quiz.setPassingScore(70);
        quiz.getQuestions().add(question(10L, 1));
        quiz.getQuestions().add(question(11L, 0));
        when(enrollmentService.getForUpdate(1L)).thenReturn(enrollment);
        when(lessonService.findQuiz(5L)).thenReturn(quiz);
    }

    @Test
    void passesWithAllCorrectAnswers() {
        var result = service.submitQuiz(5L, request(1, 0));
        assertThat(result.correctAnswers()).isEqualTo(2);
        assertThat(result.scorePercent()).isEqualTo(100);
        assertThat(result.passed()).isTrue();
        verify(enrollmentService).saveProgress(enrollment, quiz, 100, true);
    }

    @Test
    void savesFailedAttemptWithoutCompletingLesson() {
        var result = service.submitQuiz(5L, request(1, 1));
        assertThat(result.scorePercent()).isEqualTo(50);
        assertThat(result.passed()).isFalse();
        verify(enrollmentService).saveProgress(enrollment, quiz, 50, false);
    }

    @Test
    void passesAtExactThreshold() {
        quiz.setPassingScore(50);
        assertThat(service.submitQuiz(5L, request(1, 1)).passed()).isTrue();
    }

    @Test
    void rejectsDuplicateAnswers() {
        var request = new SubmitQuizRequest(1L, List.of(new QuizAnswerRequest(10L, 1),
                new QuizAnswerRequest(10L, 1)));
        assertThatThrownBy(() -> service.submitQuiz(5L, request)).isInstanceOf(IllegalArgumentException.class);
        verify(enrollmentService, never()).saveProgress(any(), any(), any(), anyBoolean());
    }

    @Test
    void rejectsUnknownQuestion() {
        var request = new SubmitQuizRequest(1L, List.of(new QuizAnswerRequest(10L, 1),
                new QuizAnswerRequest(99L, 0)));
        assertThatThrownBy(() -> service.submitQuiz(5L, request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOptionOutsideRange() {
        assertThatThrownBy(() -> service.submitQuiz(5L, request(2, 0))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeOptionBeforeSavingProgress() {
        assertThatThrownBy(() -> service.submitQuiz(5L, request(-1, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(enrollmentService, never()).saveProgress(any(), any(), any(), anyBoolean());
    }

    @Test
    void rejectsEmptyQuizBeforeCalculatingScore() {
        quiz.getQuestions().clear();
        assertThatThrownBy(() -> service.submitQuiz(5L, new SubmitQuizRequest(1L, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        verify(enrollmentService, never()).saveProgress(any(), any(), any(), anyBoolean());
    }

    @Test
    void rejectsMissingAnswer() {
        var request = new SubmitQuizRequest(1L, List.of(new QuizAnswerRequest(10L, 1)));
        assertThatThrownBy(() -> service.submitQuiz(5L, request)).isInstanceOf(IllegalArgumentException.class);
    }

    private SubmitQuizRequest request(int first, int second) {
        return new SubmitQuizRequest(1L, List.of(new QuizAnswerRequest(10L, first),
                new QuizAnswerRequest(11L, second)));
    }

    private Question question(Long id, int correct) {
        Question question = new Question();
        question.setId(id);
        question.setOptions(List.of("Нет", "Да"));
        question.setCorrectOptionIndex(correct);
        return question;
    }
}
