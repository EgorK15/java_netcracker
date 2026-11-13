package com.example.lms.service;

import com.example.lms.dto.CreateLessonRequest;
import com.example.lms.dto.CreateQuestionRequest;
import com.example.lms.dto.LessonResponse;
import com.example.lms.model.Course;
import com.example.lms.model.Lesson;
import com.example.lms.model.QuizLesson;
import com.example.lms.model.TextLesson;
import com.example.lms.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {
    @Mock LessonRepository lessons;
    @Mock CourseService courses;
    @InjectMocks LessonService service;
    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        when(courses.getForUpdate(1L)).thenReturn(course);
    }

    @Test
    void placesNewTextLessonAfterLastExistingIndex() {
        Lesson first = new TextLesson();
        first.setOrderIndex(4);
        course.getLessons().add(first);
        when(lessons.saveAndFlush(any(Lesson.class))).thenAnswer(call -> call.getArgument(0));

        var result = (LessonResponse.Text) service.createLesson(1L, new CreateLessonRequest.Text("Текст", "# Java", 5));

        assertThat(result.orderIndex()).isEqualTo(5);
        assertThat(result.content()).isEqualTo("# Java");
        assertThat(course.getLessons().getLast().getCourse()).isSameAs(course);
    }

    @Test
    void rejectsVideoUrlWithUnsupportedScheme() {
        assertThatThrownBy(() -> service.createLesson(1L, new CreateLessonRequest.Video("Видео", "ftp://example.com/v", 5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(course.getLessons()).isEmpty();
        verify(lessons, never()).saveAndFlush(any());
    }

    @Test
    void rejectsVideoUrlWithoutHost() {
        assertThatThrownBy(() -> service.createLesson(1L, new CreateLessonRequest.Video("Видео", "https:/video", 5)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(lessons, never()).saveAndFlush(any());
    }

    @Test
    void acceptsHttpVideo() {
        when(lessons.saveAndFlush(any(Lesson.class))).thenAnswer(call -> call.getArgument(0));
        var result = (LessonResponse.Video) service.createLesson(1L,
                new CreateLessonRequest.Video("Видео", "http://example.com/video", 5));
        assertThat(result.videoUrl()).isEqualTo("http://example.com/video");
    }

    @Test
    void rejectsCorrectAnswerOutsideTheOptions() {
        var question = new CreateQuestionRequest("Вопрос", List.of("А", "Б"), 2);

        assertThatThrownBy(() -> service.createLesson(1L, new CreateLessonRequest.Quiz("Тест", List.of(question), 70, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(course.getLessons()).isEmpty();
        verify(lessons, never()).saveAndFlush(any());
    }

    @Test
    void bindsQuizQuestionsAndEstimatesTimeFromTheirCount() {
        var question = new CreateQuestionRequest("Вопрос", List.of("А", "Б"), 1);
        when(lessons.saveAndFlush(any(Lesson.class))).thenAnswer(call -> call.getArgument(0));

        var result = (LessonResponse.Quiz) service.createLesson(1L, new CreateLessonRequest.Quiz("Тест", List.of(question), 70, null));

        var quiz = (QuizLesson) course.getLessons().getFirst();
        assertThat(quiz.getQuestions().getFirst().getQuiz()).isSameAs(quiz);
        assertThat(quiz.getQuestions().getFirst().isCorrectAnswer(1)).isTrue();
        assertThat(result.estimatedMinutes()).isEqualTo(1);
        assertThat(result.questions().getFirst().options()).containsExactly("А", "Б");
    }

    @Test
    void usesExplicitQuizTimeLimitWhenPresent() {
        var question = new CreateQuestionRequest("Вопрос", List.of("А", "Б"), 1);
        when(lessons.saveAndFlush(any(Lesson.class))).thenAnswer(call -> call.getArgument(0));
        var result = (LessonResponse.Quiz) service.createLesson(1L,
                new CreateLessonRequest.Quiz("Тест", List.of(question), 70, 12));
        assertThat(result.estimatedMinutes()).isEqualTo(12);
    }

    @Test
    void rejectsChangingLessonType() {
        when(lessons.findCourseId(2L)).thenReturn(Optional.of(1L));
        when(lessons.findById(2L)).thenReturn(Optional.of(new TextLesson()));

        assertThatThrownBy(() -> service.updateLesson(2L, new CreateLessonRequest.Video("Видео", "https://example.com/v", 5)))
                .isInstanceOf(IllegalStateException.class);
        verify(lessons, never()).flush();
    }
}
