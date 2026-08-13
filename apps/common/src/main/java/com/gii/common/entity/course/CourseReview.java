package com.gii.common.entity.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.entity.user.User;
import com.gii.common.enums.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "course_reviews",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_course_reviews_course_user",
          columnNames = {"course_id", "user_id"})
    })
public class CourseReview extends BaseUuidEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "rating", nullable = false)
  private Integer rating;

  @Column(name = "review_text", nullable = false, length = 5000)
  private String reviewText;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private ReviewStatus status = ReviewStatus.PENDING;
}
