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
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
    private final UserService userService;
    private final EnrollmentService enrollmentService;
    private final CertificateService certificateService;

    @Operation(summary = "Зарегистрировать студента")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateStudentRequest request) {
        return userService.createStudent(request);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.getStudent(id);
    }

    @GetMapping("/{id}/enrollments")
    public List<EnrollmentResponse> enrollments(@PathVariable Long id) {
        return enrollmentService.getStudentEnrollments(id);
    }

    @GetMapping("/{id}/certificates")
    public List<CertificateResponse> certificates(@PathVariable Long id) {
        return certificateService.getStudentCertificates(id);
    }
}
