package com.example.lms.dto;

import java.time.LocalDate;

public record CertificateResponse(Long id, Long studentId, String studentName, Long courseId,
                                  String courseTitle, LocalDate issuedAt, String uniqueCode) {
}
