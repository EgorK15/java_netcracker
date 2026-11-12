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
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
public class InstructorController {
    private final UserService userService;
    private final CourseService courseService;

    @Operation(summary = "Зарегистрировать инструктора")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateInstructorRequest request) {
        return userService.createInstructor(request);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.getInstructor(id);
    }

    @GetMapping("/{id}/courses")
    public List<CourseResponse> courses(@PathVariable Long id) {
        return courseService.getInstructorCourses(id);
    }
}
