package com.example.lms.dto;

import lombok.Builder;
import com.example.lms.enums.EnrollmentStatus;
import java.time.LocalDateTime;

@Builder
public record EnrollmentResponse(Long id, Long studentId, Long courseId, String courseTitle,
                                 EnrollmentStatus status, int progressPercent,
                                 LocalDateTime enrolledAt, LocalDateTime completedAt) {
}
