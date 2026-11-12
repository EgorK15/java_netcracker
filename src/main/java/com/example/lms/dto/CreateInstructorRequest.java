package com.example.lms.dto;

import jakarta.validation.constraints.*;

public record CreateInstructorRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 2000) String bio) {
}
