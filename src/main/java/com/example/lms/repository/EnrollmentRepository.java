package com.example.lms.repository;

import com.example.lms.model.Enrollment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    @EntityGraph(attributePaths = {"student", "course", "progress"})
    List<Enrollment> findByStudentId(Long studentId);

    @Query("select e from Enrollment e left join fetch e.progress where e.id = :id")
    Optional<Enrollment> findWithProgressById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enrollment e where e.id = :id")
    Optional<Enrollment> findForUpdate(@Param("id") Long id);
}
