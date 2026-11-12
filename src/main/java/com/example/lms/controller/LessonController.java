package com.example.lms.controller;

import com.example.lms.dto.*;
import com.example.lms.service.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;
    private final QuizService quizService;

    @Operation(summary = "Добавить урок в черновик курса")
    @PostMapping("/courses/{courseId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse create(@PathVariable Long courseId, @Valid @RequestBody CreateLessonRequest request) {
        return lessonService.createLesson(courseId, request);
    }

    @GetMapping("/courses/{courseId}/lessons")
    public List<LessonResponse> list(@PathVariable Long courseId) {
        return lessonService.getLessons(courseId);
    }

    @GetMapping("/lessons/{id}")
    public LessonResponse get(@PathVariable Long id) {
        return lessonService.getLesson(id);
    }

    @PutMapping("/lessons/{id}")
    public LessonResponse update(@PathVariable Long id, @Valid @RequestBody CreateLessonRequest request) {
        return lessonService.updateLesson(id, request);
    }

    @DeleteMapping("/lessons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        lessonService.deleteLesson(id);
    }

    @Operation(summary = "Получить вопросы без правильных ответов")
    @GetMapping("/lessons/{id}/questions")
    public List<QuestionResponse> questions(@PathVariable Long id) {
        return lessonService.getQuestions(id);
    }

    @Operation(summary = "Сдать тест", description = "enrollmentId и один ответ на каждый вопрос; индексы с нуля")
    @PostMapping("/lessons/{id}/submit")
    public QuizResultResponse submit(@PathVariable Long id, @Valid @RequestBody SubmitQuizRequest request) {
        return quizService.submitQuiz(id, request);
    }
}
