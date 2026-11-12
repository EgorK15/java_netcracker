package com.example.lms.repository;

import com.example.lms.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    @Query("select l.course.id from Lesson l where l.id = :id")
    Optional<Long> findCourseId(@Param("id") Long id);
}
