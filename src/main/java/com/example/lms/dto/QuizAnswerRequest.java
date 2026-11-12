package com.example.lms.dto;

import jakarta.validation.constraints.*;

public record QuizAnswerRequest(@NotNull @Positive Long questionId,
                                @NotNull @Min(0) Integer selectedOption) {
}
