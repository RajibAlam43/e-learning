package com.gii.common.entity.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Reusable curriculum content shared by every {@link Course} offering built on it. */
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "course_templates")
public class CourseTemplate extends BaseUuidEntity {

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "title_en")
  private String titleEn;

  @Column(name = "thumbnail_object_key")
  private String thumbnailObjectKey;

  @Column(name = "youtube_video_id", length = 11)
  private String youtubeVideoId;

  @Column(name = "short_description")
  private String shortDescription;

  @Column(name = "short_description_en")
  private String shortDescriptionEn;

  @Column(name = "description")
  private String description;

  @Column(name = "description_en")
  private String descriptionEn;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "highlights", columnDefinition = "jsonb")
  private List<String> highlights;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "highlights_en", columnDefinition = "jsonb")
  private List<String> highlightsEn;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "course_outcomes", columnDefinition = "jsonb")
  private List<String> courseOutcomes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "course_outcomes_en", columnDefinition = "jsonb")
  private List<String> courseOutcomesEn;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "requirements", columnDefinition = "jsonb")
  private List<String> requirements;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "requirements_en", columnDefinition = "jsonb")
  private List<String> requirementsEn;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "prerequisites", columnDefinition = "jsonb")
  private List<String> prerequisites;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "prerequisites_en", columnDefinition = "jsonb")
  private List<String> prerequisitesEn;

  @Enumerated(EnumType.STRING)
  @Column(name = "level", nullable = false, length = 30)
  @Builder.Default
  private CourseLevel level = CourseLevel.BEGINNER;

  @Enumerated(EnumType.STRING)
  @Column(name = "language", nullable = false, length = 20)
  @Builder.Default
  private CourseLanguage language = CourseLanguage.BN;

  @Column(name = "live_session_count", nullable = false)
  @Builder.Default
  private Integer liveSessionCount = 0;

  @Column(name = "quiz_count", nullable = false)
  @Builder.Default
  private Integer quizCount = 0;

  @Column(name = "recorded_hours_count", nullable = false)
  @Builder.Default
  private Integer recordedHoursCount = 0;

  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "preview_lesson_id")
  private Lesson previewLesson;

  @Column(name = "estimated_duration_minutes")
  private Integer estimatedDurationMinutes;

  @Column(name = "target_audience", columnDefinition = "text")
  private String targetAudience;

  @Column(name = "target_audience_en", columnDefinition = "text")
  private String targetAudienceEn;
}
