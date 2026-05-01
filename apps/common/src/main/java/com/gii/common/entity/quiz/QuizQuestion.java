package com.gii.common.entity.quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.enums.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
    name = "quiz_questions",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_quiz_questions_quiz_position",
          columnNames = {"quiz_id", "position"})
    })
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
  @Builder.Default
  private QuestionType questionType = QuestionType.MCQ;

  @Column(name = "points", nullable = false)
  @Builder.Default
  private Integer points = 1;

  @Column(name = "explanation_text")
  private String explanationText;
}
