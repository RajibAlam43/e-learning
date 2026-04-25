package com.gii.common.model.quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quiz_choices")
public class QuizChoice {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private java.util.UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "choice_text", nullable = false)
    private String choiceText;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;
}