package com.example.lms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record SubmitQuizRequest(@NotNull @Positive Long enrollmentId,
                                @NotNull @Size(min = 1, max = 100)
                                List<@NotNull @Valid QuizAnswerRequest> answers) {
}
