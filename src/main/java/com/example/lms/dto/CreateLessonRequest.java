package com.example.lms.dto;

import com.example.lms.enums.LessonType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateLessonRequest.Video.class, name = "VIDEO"),
        @JsonSubTypes.Type(value = CreateLessonRequest.Text.class, name = "TEXT"),
        @JsonSubTypes.Type(value = CreateLessonRequest.Quiz.class, name = "QUIZ")
})
public sealed interface CreateLessonRequest {
    String title();
    LessonType type();

    @Schema(name = "VideoLessonRequest")
    record Video(@NotBlank @Size(max = 200) String title,
                 @NotBlank @Size(max = 2000) String videoUrl,
                 @NotNull @Positive Integer durationMinutes) implements CreateLessonRequest {
        public LessonType type() { return LessonType.VIDEO; }
    }

    @Schema(name = "TextLessonRequest")
    record Text(@NotBlank @Size(max = 200) String title,
                @NotBlank @Size(max = 100000) String content,
                @NotNull @Positive Integer readingTimeMinutes) implements CreateLessonRequest {
        public LessonType type() { return LessonType.TEXT; }
    }

    @Schema(name = "QuizLessonRequest")
    record Quiz(@NotBlank @Size(max = 200) String title,
                @NotNull @Size(min = 1, max = 100) List<@NotNull @Valid CreateQuestionRequest> questions,
                @NotNull @Min(1) @Max(100) Integer passingScore,
                @Positive Integer timeLimitMinutes) implements CreateLessonRequest {
        public LessonType type() { return LessonType.QUIZ; }
    }
}
