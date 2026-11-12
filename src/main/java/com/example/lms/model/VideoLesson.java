package com.example.lms.model;

import com.example.lms.enums.LessonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("VIDEO")
@Getter
@Setter
public class VideoLesson extends Lesson {
    @Column(length = 2000)
    private String videoUrl;
    private int durationMinutes;

    @Override
    public LessonType getType() {
        return LessonType.VIDEO;
    }

    @Override
    public int getEstimatedMinutes() {
        return durationMinutes;
    }
}
