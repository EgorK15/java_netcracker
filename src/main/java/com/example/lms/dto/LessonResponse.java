package com.example.lms.dto;

import lombok.Builder;
import com.example.lms.enums.LessonType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(oneOf = {LessonResponse.Video.class, LessonResponse.Text.class, LessonResponse.Quiz.class})
public sealed interface LessonResponse {
    Long id();

    @Builder
    @Schema(name = "VideoLessonResponse")
    record Video(Long id, String title, LessonType type, int orderIndex, int estimatedMinutes,
                 String videoUrl, int durationMinutes) implements LessonResponse {
    }

    @Builder
    @Schema(name = "TextLessonResponse")
    record Text(Long id, String title, LessonType type, int orderIndex, int estimatedMinutes,
                String content, int readingTimeMinutes) implements LessonResponse {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    @Schema(name = "QuizLessonResponse")
    record Quiz(Long id, String title, LessonType type, int orderIndex, int estimatedMinutes,
                int passingScore, Integer timeLimitMinutes, List<QuestionResponse> questions) implements LessonResponse {
    }
}
