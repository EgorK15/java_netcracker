package com.example.lms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 2000)
    private String text;
    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "option_index")
    @Column(name = "option_text", nullable = false, length = 1000)
    private List<String> options = new ArrayList<>();
    @Column(nullable = false)
    private int correctOptionIndex;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private QuizLesson quiz;

    public boolean isCorrectAnswer(int index) {
        return index == correctOptionIndex;
    }
}
