package com.example.lms.dto;

import lombok.Builder;
import com.example.lms.enums.CourseStatus;
import java.util.List;

@Builder
public record CourseDetailsResponse(Long id, String title, String description, String authorName,
                                    CourseStatus status, int totalLessons, long duration,
                                    List<LessonResponse> lessons) {
}
