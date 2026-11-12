package com.example.lms.model;

import com.example.lms.enums.LessonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("QUIZ")
@Getter
@Setter
public class QuizLesson extends Lesson {
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Question> questions = new ArrayList<>();
    private int passingScore;
    private Integer timeLimitMinutes;

    @Override
    public LessonType getType() {
        return LessonType.QUIZ;
    }

    @Override
    public int getEstimatedMinutes() {
        return timeLimitMinutes != null ? timeLimitMinutes : questions.size();
    }
}
