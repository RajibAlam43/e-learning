package com.gii.common.model.quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.model.enums.QuestionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "quiz_questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_quiz_questions_quiz_position", columnNames = {"quiz_id", "position"})
        }
)
public class QuizQuestion {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private java.util.UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType = QuestionType.mcq;

    @Column(name = "points", nullable = false)
    private Integer points = 1;
}