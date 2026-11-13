package com.example.lms.dto;

import lombok.Builder;
import com.example.lms.enums.EnrollmentStatus;
import java.util.List;

@Builder
public record ProgressResponse(Long enrollmentId, String courseTitle, int progressPercent,
                               int completedLessons, int totalLessons, EnrollmentStatus status,
                               List<LessonProgressResponse> lessons) {
}
