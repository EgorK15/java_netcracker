package com.example.lms.service;

import com.example.lms.model.Certificate;
import com.example.lms.model.Course;
import com.example.lms.model.Enrollment;
import com.example.lms.model.Student;
import com.example.lms.repository.CertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {
    @Mock CertificateRepository certificates;
    @Mock UserService users;
    @InjectMocks CertificateService service;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        Student student = new Student();
        student.setId(1L);
        student.setFirstName("Иван\r\nПётр");
        student.setLastName("Иванов");
        Course course = new Course();
        course.setId(2L);
        course.setTitle("Курс\nJava");
        enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
    }

    @Test
    void issuesCertificateForStudentAndCourseWithUuid() {
        service.issueCertificate(enrollment);

        var saved = ArgumentCaptor.forClass(Certificate.class);
        verify(certificates).save(saved.capture());
        var certificate = saved.getValue();
        assertThat(certificate.getStudent()).isSameAs(enrollment.getStudent());
        assertThat(certificate.getCourse()).isSameAs(enrollment.getCourse());
        assertThat(UUID.fromString(certificate.getUniqueCode()).version()).isEqualTo(4);
    }

    @Test
    void skipsIssuingDuplicateCertificate() {
        when(certificates.existsByStudentIdAndCourseId(1L, 2L)).thenReturn(true);
        service.issueCertificate(enrollment);
        verify(certificates, never()).save(any());
    }

    @Test
    void rendersUtf8CertificateWithSingleLineFields() {
        Certificate certificate = new Certificate();
        certificate.setStudent(enrollment.getStudent());
        certificate.setCourse(enrollment.getCourse());
        certificate.setIssuedAt(LocalDate.of(2026, 1, 15));
        when(certificates.findById(3L)).thenReturn(Optional.of(certificate));

        String text = new String(service.download(3L), StandardCharsets.UTF_8);

        assertThat(text).contains("Иван Пётр Иванов", "\"Курс Java\"", "15 января 2026", certificate.getUniqueCode());
        assertThat(enrollment.getCourse().getTitle()).isEqualTo("Курс\nJava");
    }
}
