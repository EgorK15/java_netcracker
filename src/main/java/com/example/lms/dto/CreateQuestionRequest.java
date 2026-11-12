package com.example.lms.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateQuestionRequest(
        @NotBlank @Size(max = 2000) String text,
        @NotNull @Size(min = 2, max = 10) List<@NotBlank @Size(max = 1000) String> options,
        @NotNull @Min(0) Integer correctOptionIndex) {
}
