package com.example.lms.dto;

import lombok.Builder;
import com.example.lms.enums.CourseStatus;

@Builder
public record CourseResponse(Long id, String title, String description, String authorName,
                             CourseStatus status, int totalLessons, long duration) {
}
