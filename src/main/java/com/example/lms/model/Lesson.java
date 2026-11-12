package com.example.lms.model;

import com.example.lms.enums.LessonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lessons")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "lesson_type")
@Getter
@Setter
public abstract class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false)
    private int orderIndex;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Course course;

    public abstract LessonType getType();
    public abstract int getEstimatedMinutes();
}
