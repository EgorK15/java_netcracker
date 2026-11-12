package com.example.lms.dto;

import jakarta.validation.constraints.*;

public record CreateEnrollmentRequest(@NotNull @Positive Long studentId,
                                      @NotNull @Positive Long courseId) {
}
