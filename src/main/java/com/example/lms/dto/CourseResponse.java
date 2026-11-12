package com.example.lms.dto;

import com.example.lms.enums.CourseStatus;

public record CourseResponse(Long id, String title, String description, String authorName,
                             CourseStatus status, int totalLessons, long duration) {
}
