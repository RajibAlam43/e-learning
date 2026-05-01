package com.gii.common.entity.enrollment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
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
@Table(name = "lesson_progress")
public class LessonProgress {

  @EmbeddedId @Builder.Default private LessonProgressId id = LessonProgressId.builder().build();

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("userId")
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("lessonId")
  @JoinColumn(name = "lesson_id", nullable = false)
  private Lesson lesson;

  @Column(name = "last_position_sec")
  private Integer lastPositionSec;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @PrePersist
  protected void onCreate() {
    if (updatedAt == null) {
      updatedAt = Instant.now();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }
}
