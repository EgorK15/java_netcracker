package com.example.lms.controller;

import com.example.lms.dto.*;
import com.example.lms.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @Operation(summary = "Записать студента на опубликованный курс")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse create(@Valid @RequestBody CreateEnrollmentRequest request) {
        return enrollmentService.enrollStudent(request.studentId(), request.courseId());
    }

    @GetMapping("/{id}")
    public ProgressResponse get(@PathVariable Long id) {
        return enrollmentService.getProgress(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        enrollmentService.dropEnrollment(id);
    }

    @Operation(summary = "Завершить обычный урок", description = "Повторное завершение не меняет прогресс")
    @PostMapping("/{id}/lessons/{lessonId}/complete")
    public ProgressResponse complete(@PathVariable Long id, @PathVariable Long lessonId) {
        return enrollmentService.completeLesson(id, lessonId);
    }

    @GetMapping("/{id}/progress")
    public ProgressResponse progress(@PathVariable Long id) {
        return enrollmentService.getProgress(id);
    }
}
