package com.example.lms.repository;

import com.example.lms.model.Certificate;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    Optional<Certificate> findByUniqueCode(String code);

    @EntityGraph(attributePaths = {"student", "course"})
    List<Certificate> findByStudentId(Long studentId);
}
