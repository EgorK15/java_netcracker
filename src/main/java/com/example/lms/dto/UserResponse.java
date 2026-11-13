package com.example.lms.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record UserResponse(Long id, String firstName, String lastName, String email,
                           LocalDateTime createdAt, String bio) {
}
