package com.gii.common.entity.quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "quiz_attempt_answers")
public class QuizAttemptAnswer {

  @EmbeddedId @Builder.Default
  private QuizAttemptAnswerId id = QuizAttemptAnswerId.builder().build();

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
