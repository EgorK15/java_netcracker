package com.example.lms.dto;

import com.example.lms.enums.LessonType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateLessonRequest.Video.class, name = "VIDEO"),
        @JsonSubTypes.Type(value = CreateLessonRequest.Text.class, name = "TEXT"),
        @JsonSubTypes.Type(value = CreateLessonRequest.Quiz.class, name = "QUIZ")
})
@Schema(discriminatorProperty = "type", discriminatorMapping = {
        @DiscriminatorMapping(value = "VIDEO", schema = CreateLessonRequest.Video.class),
        @DiscriminatorMapping(value = "TEXT", schema = CreateLessonRequest.Text.class),
        @DiscriminatorMapping(value = "QUIZ", schema = CreateLessonRequest.Quiz.class)
})
public sealed interface CreateLessonRequest {
    String title();
    LessonType type();

    @Schema(name = "VideoLessonRequest")
    record Video(@NotBlank @Size(max = 200) String title,
                 @NotBlank @Size(max = 2000) String videoUrl,
                 @NotNull @Positive Integer durationMinutes) implements CreateLessonRequest {
        @JsonProperty("type")
        @Schema(allowableValues = "VIDEO", requiredMode = Schema.RequiredMode.REQUIRED)
        public LessonType type() { return LessonType.VIDEO; }
    }

    @Schema(name = "TextLessonRequest")
    record Text(@NotBlank @Size(max = 200) String title,
                @NotBlank @Size(max = 100000) String content,
                @NotNull @Positive Integer readingTimeMinutes) implements CreateLessonRequest {
        @JsonProperty("type")
        @Schema(allowableValues = "TEXT", requiredMode = Schema.RequiredMode.REQUIRED)
        public LessonType type() { return LessonType.TEXT; }
    }

    @Schema(name = "QuizLessonRequest")
    record Quiz(@NotBlank @Size(max = 200) String title,
                @NotNull @Size(min = 1, max = 100) List<@NotNull @Valid CreateQuestionRequest> questions,
                @NotNull @Min(1) @Max(100) Integer passingScore,
                @Positive Integer timeLimitMinutes) implements CreateLessonRequest {
        @JsonProperty("type")
        @Schema(allowableValues = "QUIZ", requiredMode = Schema.RequiredMode.REQUIRED)
        public LessonType type() { return LessonType.QUIZ; }
    }
}
