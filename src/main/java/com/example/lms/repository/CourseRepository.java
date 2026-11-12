package com.example.lms.repository;

import com.example.lms.enums.CourseStatus;
import com.example.lms.model.Course;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @EntityGraph(attributePaths = {"author", "lessons"})
    List<Course> findByStatus(CourseStatus status);

    @EntityGraph(attributePaths = {"author", "lessons"})
    List<Course> findByAuthorId(Long authorId);

    @EntityGraph(attributePaths = {"author", "lessons"})
    List<Course> findByTitleContainingIgnoreCase(String title);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findForUpdate(@Param("id") Long id);
}
