package com.example.lms.model;

import com.example.lms.enums.LessonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("TEXT")
@Getter
@Setter
public class TextLesson extends Lesson {
    @Column(length = 100000)
    private String content;
    private int readingTimeMinutes;

    @Override
    public LessonType getType() {
        return LessonType.TEXT;
    }

    @Override
    public int getEstimatedMinutes() {
        return readingTimeMinutes;
    }
}
