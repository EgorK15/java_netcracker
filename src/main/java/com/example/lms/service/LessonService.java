package com.example.lms.service;

import com.example.lms.dto.*;
import com.example.lms.exception.NotFoundException;
import com.example.lms.model.*;
import com.example.lms.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
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
        Lesson lesson = lessonRepository.findById(id).orElseThrow(() -> new NotFoundException("Урок не найден"));
        return Hibernate.unproxy(lesson, Lesson.class);
    }

    public LessonResponse getLesson(Long id) {
        return Mapper.lesson(findLesson(id));
    }

    @Transactional
    public LessonResponse updateLesson(Long id, CreateLessonRequest request) {
        Course course = lockCourseForLesson(id);
        Lesson lesson = findLesson(id);
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
        Course course = lockCourseForLesson(id);
        Lesson lesson = findLesson(id);
        courseService.requireDraft(course);
        course.getLessons().remove(lesson);
        lessonRepository.delete(lesson);
    }

    private Course lockCourseForLesson(Long id) {
        Long courseId = lessonRepository.findCourseId(id)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));
        return courseService.getForUpdate(courseId);
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
        switch (request) {
            case CreateLessonRequest.Video data -> {
                VideoLesson video = (VideoLesson) lesson;
                URI uri = URI.create(data.videoUrl());
                if (uri.getHost() == null || !("https".equalsIgnoreCase(uri.getScheme())
                        || "http".equalsIgnoreCase(uri.getScheme()))) {
                    throw new IllegalArgumentException("videoUrl должен быть HTTP или HTTPS адресом");
                }
                video.setVideoUrl(data.videoUrl());
                video.setDurationMinutes(data.durationMinutes());
            }
            case CreateLessonRequest.Text data -> {
                TextLesson text = (TextLesson) lesson;
                text.setContent(data.content());
                text.setReadingTimeMinutes(data.readingTimeMinutes());
            }
            case CreateLessonRequest.Quiz data -> {
                QuizLesson quiz = (QuizLesson) lesson;
                List<Question> questions = new ArrayList<>();
                for (CreateQuestionRequest item : data.questions()) {
                    if (item.correctOptionIndex() >= item.options().size()) {
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
                quiz.setPassingScore(data.passingScore());
                quiz.setTimeLimitMinutes(data.timeLimitMinutes());
            }
        }
    }
}
