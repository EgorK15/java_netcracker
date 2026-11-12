package com.example.lms.service;

import com.example.lms.dto.*;
import com.example.lms.exception.NotFoundException;
import com.example.lms.model.*;
import com.example.lms.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {
    private final LessonRepository lessonRepository;
    private final CourseService courseService;

    @Transactional
    public LessonResponse createLesson(Long courseId, CreateLessonRequest request) {
        Course course = courseService.getForUpdate(courseId);
        courseService.requireDraft(course);
        Lesson lesson = switch (request.type()) {
            case VIDEO -> new VideoLesson();
            case TEXT -> new TextLesson();
            case QUIZ -> new QuizLesson();
        };
        fillLesson(lesson, request);
        lesson.setCourse(course);
        int nextIndex = course.getLessons().stream().mapToInt(Lesson::getOrderIndex).max().orElse(0) + 1;
        lesson.setOrderIndex(nextIndex);
        course.getLessons().add(lesson);
        return Mapper.lesson(lessonRepository.saveAndFlush(lesson));
    }

    public List<LessonResponse> getLessons(Long courseId) {
        courseService.getCourseById(courseId);
        return lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)
                .stream().map(Mapper::lesson).toList();
    }

    public Lesson findLesson(Long id) {
        return lessonRepository.findById(id).orElseThrow(() -> new NotFoundException("Урок не найден"));
    }

    public LessonResponse getLesson(Long id) {
        return Mapper.lesson(findLesson(id));
    }

    @Transactional
    public LessonResponse updateLesson(Long id, CreateLessonRequest request) {
        Lesson lesson = findLesson(id);
        Course course = courseService.getForUpdate(lesson.getCourse().getId());
        courseService.requireDraft(course);
        if (lesson.getType() != request.type()) {
            throw new IllegalStateException("Менять тип урока нельзя: создайте новый урок");
        }
        fillLesson(lesson, request);
        lessonRepository.flush();
        return Mapper.lesson(lesson);
    }

    @Transactional
    public void deleteLesson(Long id) {
        Lesson lesson = findLesson(id);
        Course course = courseService.getForUpdate(lesson.getCourse().getId());
        courseService.requireDraft(course);
        course.getLessons().remove(lesson);
        lessonRepository.delete(lesson);
    }

    public QuizLesson findQuiz(Long id) {
        Lesson lesson = findLesson(id);
        if (!(lesson instanceof QuizLesson quiz)) {
            throw new IllegalStateException("Этот урок не является тестом");
        }
        return quiz;
    }

    public List<QuestionResponse> getQuestions(Long id) {
        return findQuiz(id).getQuestions().stream().map(Mapper::question).toList();
    }

    private void fillLesson(Lesson lesson, CreateLessonRequest request) {
        lesson.setTitle(request.title().strip());
        if (lesson instanceof VideoLesson video) {
            requireText(request.videoUrl(), "Для видео нужен videoUrl");
            URI uri = URI.create(request.videoUrl());
            if (uri.getHost() == null || !("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException("videoUrl должен быть HTTP или HTTPS адресом");
            }
            requirePositive(request.durationMinutes(), "Укажите durationMinutes больше нуля");
            video.setVideoUrl(request.videoUrl());
            video.setDurationMinutes(request.durationMinutes());
        } else if (lesson instanceof TextLesson text) {
            requireText(request.content(), "Для текстового урока нужен content");
            requirePositive(request.readingTimeMinutes(), "Укажите readingTimeMinutes больше нуля");
            text.setContent(request.content());
            text.setReadingTimeMinutes(request.readingTimeMinutes());
        } else if (lesson instanceof QuizLesson quiz) {
            if (request.questions() == null || request.questions().isEmpty()) {
                throw new IllegalArgumentException("В тесте должен быть хотя бы один вопрос");
            }
            if (request.passingScore() == null || request.passingScore() < 1 || request.passingScore() > 100) {
                throw new IllegalArgumentException("passingScore должен быть от 1 до 100");
            }
            List<Question> questions = new ArrayList<>();
            for (CreateQuestionRequest item : request.questions()) {
                if (item.correctOptionIndex() < 0 || item.correctOptionIndex() >= item.options().size()) {
                    throw new IllegalArgumentException("Индекс правильного ответа выходит за границы вариантов");
                }
                Question question = new Question();
                question.setText(item.text());
                question.setOptions(new ArrayList<>(item.options()));
                question.setCorrectOptionIndex(item.correctOptionIndex());
                question.setQuiz(quiz);
                questions.add(question);
            }
            quiz.getQuestions().clear();
            quiz.getQuestions().addAll(questions);
            quiz.setPassingScore(request.passingScore());
            quiz.setTimeLimitMinutes(request.timeLimitMinutes());
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requirePositive(Integer value, String message) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(message);
        }
    }
}
