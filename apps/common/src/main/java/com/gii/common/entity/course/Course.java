package com.gii.common.entity.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A concrete, purchasable delivery of a versioned course template. Public APIs intentionally expose
 * this entity as a course and expose its UUID as {@code courseId}.
 */
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "course_offerings")
public class Course extends BaseUuidEntity {

  @JsonIgnore
  @ManyToOne(
      fetch = FetchType.EAGER,
      optional = false,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "course_template_version_id", nullable = false)
  private CourseTemplateVersion templateVersion;

  @Column(name = "slug", nullable = false, unique = true)
  private String slug;

  @Column(name = "name")
  private String name;

  @Column(name = "price_bdt", nullable = false)
  @Builder.Default
  private BigDecimal priceBdt = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "study_mode", nullable = false, length = 30)
  @Builder.Default
  private StudyMode studyMode = StudyMode.COHORT_BASED;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private PublishStatus status = PublishStatus.DRAFT;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "is_featured", nullable = false)
  @Builder.Default
  private Boolean isFeatured = false;

  @Column(name = "featured_position")
  private Integer featuredPosition;

  @Column(name = "featured_at")
  private Instant featuredAt;

  @Column(name = "is_free", nullable = false)
  @Builder.Default
  private Boolean isFree = false;

  @Column(name = "timezone", length = 80)
  private String timezone;

  @Column(name = "enrollment_starts_at")
  private Instant enrollmentStartsAt;

  @Column(name = "enrollment_ends_at")
  private Instant enrollmentEndsAt;

  @Column(name = "starts_at")
  private Instant startsAt;

  @Column(name = "ends_at")
  private Instant endsAt;

  @Column(name = "capacity")
  private Integer capacity;

  @Column(name = "access_duration_days")
  private Integer accessDurationDays;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  public String getTitle() {
    return templateVersion.getTitle();
  }

  public void setTitle(String title) {
    templateVersion.setTitle(title);
  }

  public String getTitleEn() {
    return templateVersion.getTitleEn();
  }

  public void setTitleEn(String value) {
    templateVersion.setTitleEn(value);
  }

  public String getThumbnailObjectKey() {
    return templateVersion.getThumbnailObjectKey();
  }

  public void setThumbnailObjectKey(String value) {
    templateVersion.setThumbnailObjectKey(value);
  }

  public String getShortDescription() {
    return templateVersion.getShortDescription();
  }

  public void setShortDescription(String value) {
    templateVersion.setShortDescription(value);
  }

  public String getShortDescriptionEn() {
    return templateVersion.getShortDescriptionEn();
  }

  public void setShortDescriptionEn(String value) {
    templateVersion.setShortDescriptionEn(value);
  }

  public String getDescription() {
    return templateVersion.getDescription();
  }

  public void setDescription(String value) {
    templateVersion.setDescription(value);
  }

  public String getDescriptionEn() {
    return templateVersion.getDescriptionEn();
  }

  public void setDescriptionEn(String value) {
    templateVersion.setDescriptionEn(value);
  }

  public List<String> getHighlights() {
    return templateVersion.getHighlights();
  }

  public void setHighlights(List<String> value) {
    templateVersion.setHighlights(value);
  }

  public List<String> getHighlightsEn() {
    return templateVersion.getHighlightsEn();
  }

  public void setHighlightsEn(List<String> value) {
    templateVersion.setHighlightsEn(value);
  }

  public List<String> getCourseOutcomes() {
    return templateVersion.getCourseOutcomes();
  }

  public void setCourseOutcomes(List<String> value) {
    templateVersion.setCourseOutcomes(value);
  }

  public List<String> getCourseOutcomesEn() {
    return templateVersion.getCourseOutcomesEn();
  }

  public void setCourseOutcomesEn(List<String> value) {
    templateVersion.setCourseOutcomesEn(value);
  }

  public List<String> getRequirements() {
    return templateVersion.getRequirements();
  }

  public void setRequirements(List<String> value) {
    templateVersion.setRequirements(value);
  }

  public List<String> getRequirementsEn() {
    return templateVersion.getRequirementsEn();
  }

  public void setRequirementsEn(List<String> value) {
    templateVersion.setRequirementsEn(value);
  }

  public List<String> getPrerequisites() {
    return templateVersion.getPrerequisites();
  }

  public void setPrerequisites(List<String> value) {
    templateVersion.setPrerequisites(value);
  }

  public List<String> getPrerequisitesEn() {
    return templateVersion.getPrerequisitesEn();
  }

  public void setPrerequisitesEn(List<String> value) {
    templateVersion.setPrerequisitesEn(value);
  }

  public CourseLevel getLevel() {
    return templateVersion.getLevel();
  }

  public void setLevel(CourseLevel value) {
    templateVersion.setLevel(value);
  }

  public CourseLanguage getLanguage() {
    return templateVersion.getLanguage();
  }

  public void setLanguage(CourseLanguage value) {
    templateVersion.setLanguage(value);
  }

  public Integer getLiveSessionCount() {
    return templateVersion.getLiveSessionCount();
  }

  public void setLiveSessionCount(Integer value) {
    templateVersion.setLiveSessionCount(value);
  }

  public Integer getQuizCount() {
    return templateVersion.getQuizCount();
  }

  public void setQuizCount(Integer value) {
    templateVersion.setQuizCount(value);
  }

  public Integer getRecordedHoursCount() {
    return templateVersion.getRecordedHoursCount();
  }

  public void setRecordedHoursCount(Integer value) {
    templateVersion.setRecordedHoursCount(value);
  }

  public Lesson getPreviewLesson() {
    return templateVersion.getPreviewLesson();
  }

  public void setPreviewLesson(Lesson value) {
    templateVersion.setPreviewLesson(value);
  }

  public Integer getEstimatedDurationMinutes() {
    return templateVersion.getEstimatedDurationMinutes();
  }

  public void setEstimatedDurationMinutes(Integer value) {
    templateVersion.setEstimatedDurationMinutes(value);
  }

  public String getTargetAudience() {
    return templateVersion.getTargetAudience();
  }

  public void setTargetAudience(String value) {
    templateVersion.setTargetAudience(value);
  }

  public String getTargetAudienceEn() {
    return templateVersion.getTargetAudienceEn();
  }

  public void setTargetAudienceEn(String value) {
    templateVersion.setTargetAudienceEn(value);
  }
}
