package com.gii.common.entity.quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
  @Builder.Default
  private Boolean isCorrect = false;
}
