package com.example.lms.dto;

import com.example.lms.enums.LessonType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LessonResponse(Long id, String title, LessonType type, int orderIndex,
                             int estimatedMinutes, String videoUrl, Integer durationMinutes,
                             String content, Integer readingTimeMinutes, Integer passingScore,
                             Integer timeLimitMinutes, List<QuestionResponse> questions) {
}
