package com.example.lms.dto;

import jakarta.validation.constraints.*;

public record UpdateCourseRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description) {
}
