package com.example.lms.controller;

import com.example.lms.dto.*;
import com.example.lms.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @Operation(summary = "Создать курс", description = "Создаёт курс в статусе DRAFT")
    @ApiResponse(responseCode = "201", description = "Курс создан", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Некорректные данные",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Инструктор не найден",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CreateCourseRequest request) {
        return courseService.createCourse(request);
    }

    @Operation(summary = "Получить опубликованные курсы")
    @GetMapping
    public List<CourseResponse> list() {
        return courseService.getPublishedCourses();
    }

    @GetMapping("/{id}")
    public CourseDetailsResponse get(@PathVariable Long id) {
        return courseService.getDetails(id);
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCourseRequest request) {
        return courseService.updateCourse(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }

    @Operation(summary = "Опубликовать черновик с уроками")
    @PatchMapping("/{id}/publish")
    public CourseResponse publish(@PathVariable Long id) {
        return courseService.publishCourse(id);
    }

    @PatchMapping("/{id}/archive")
    public CourseResponse archive(@PathVariable Long id) {
        return courseService.archiveCourse(id);
    }
}
