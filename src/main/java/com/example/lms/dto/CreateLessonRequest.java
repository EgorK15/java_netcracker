package com.example.lms.dto;

import com.example.lms.enums.LessonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateLessonRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull LessonType type,
        @Size(max = 2000) String videoUrl,
        @Min(1) Integer durationMinutes,
        @Size(max = 100000) String content,
        @Min(1) Integer readingTimeMinutes,
        @Size(min = 1, max = 100) List<@NotNull @Valid CreateQuestionRequest> questions,
        @Min(1) @Max(100) Integer passingScore,
        @Min(1) Integer timeLimitMinutes) {
}
