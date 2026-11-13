package com.example.lms.dto;

import lombok.Builder;
import java.time.LocalDate;

@Builder
public record CertificateResponse(Long id, Long studentId, String studentName, Long courseId,
                                  String courseTitle, LocalDate issuedAt, String uniqueCode) {
}
