package com.example.lms.model;

import com.example.lms.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(length = 2000)
    private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Instructor author;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.DRAFT;
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC, id ASC")
    private List<Lesson> lessons = new ArrayList<>();
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime publishedAt;

    public int getTotalLessons() {
        return lessons.size();
    }

    public int getTotalEstimatedMinutes() {
        int total = 0;
        for (Lesson lesson : lessons) {
            total += lesson.getEstimatedMinutes();
        }
        return total;
    }

    public boolean isAvailableForEnrollment() {
        return status == CourseStatus.PUBLISHED;
    }

    public void publish() {
        if (status != CourseStatus.DRAFT || lessons.isEmpty()) {
            throw new IllegalStateException("Опубликовать можно только черновик с уроками");
        }
        status = CourseStatus.PUBLISHED;
        publishedAt = LocalDateTime.now();
    }
}
