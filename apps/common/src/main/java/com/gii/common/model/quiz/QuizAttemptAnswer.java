package com.gii.common.model.quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quiz_attempt_answers")
public class QuizAttemptAnswer {

    @EmbeddedId
    private QuizAttemptAnswerId id = new QuizAttemptAnswerId();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("attemptId")
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "choice_id", nullable = false)
    private QuizChoice choice;
}