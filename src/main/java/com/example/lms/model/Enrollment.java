package com.example.lms.model;

import com.example.lms.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
@Getter
@Setter
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Course course;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;
    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LessonProgress> progress = new HashSet<>();
    @Column(nullable = false, updatable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    public int getCompletedLessons() {
        int count = 0;
        for (LessonProgress item : progress) {
            if (item.getCompletedAt() != null) {
                count++;
            }
        }
        return count;
    }

    public int getProgressPercent() {
        int total = course.getTotalLessons();
        return total == 0 ? 0 : getCompletedLessons() * 100 / total;
    }
}
