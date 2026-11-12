package com.example.lms.service;

import com.example.lms.dto.*;
import com.example.lms.exception.NotFoundException;
import com.example.lms.model.*;
import com.example.lms.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {
    private final CertificateRepository certificateRepository;
    private final UserService userService;

    @Transactional
    public void issueCertificate(Enrollment enrollment) {
        Long studentId = enrollment.getStudent().getId();
        Long courseId = enrollment.getCourse().getId();
        if (!certificateRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            Certificate certificate = new Certificate();
            certificate.setStudent(enrollment.getStudent());
            certificate.setCourse(enrollment.getCourse());
            certificateRepository.save(certificate);
        }
    }

    public List<CertificateResponse> getStudentCertificates(Long id) {
        userService.findStudent(id);
        return certificateRepository.findByStudentId(id).stream().map(Mapper::certificate).toList();
    }

    public CertificateResponse getByCode(String code) {
        return Mapper.certificate(certificateRepository.findByUniqueCode(code)
                .orElseThrow(() -> new NotFoundException("Сертификат не найден")));
    }

    public byte[] download(Long id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Сертификат не найден"));
        DateTimeFormatter format = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"));
        String text = """
                ═══════════════════════════════════════════
                       СЕРТИФИКАТ О ПРОХОЖДЕНИИ
                ═══════════════════════════════════════════

                Настоящим подтверждается, что

                %s

                успешно завершил(а) курс

                "%s"

                Дата выдачи: %s
                Код сертификата: %s

                ═══════════════════════════════════════════
                """.formatted(certificate.getStudent().getFullName(), certificate.getCourse().getTitle(),
                certificate.getIssuedAt().format(format), certificate.getUniqueCode());
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
