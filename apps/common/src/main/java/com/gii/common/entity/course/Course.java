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
 * A concrete, purchasable delivery of a course template. Public APIs intentionally expose
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
  @JoinColumn(name = "course_template_id", nullable = false)
  private CourseTemplate template;

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
    return template.getTitle();
  }

  public void setTitle(String title) {
    template.setTitle(title);
  }

  public String getTitleEn() {
    return template.getTitleEn();
  }

  public void setTitleEn(String value) {
    template.setTitleEn(value);
  }

  public String getThumbnailObjectKey() {
    return template.getThumbnailObjectKey();
  }

  public void setThumbnailObjectKey(String value) {
    template.setThumbnailObjectKey(value);
  }

  public String getShortDescription() {
    return template.getShortDescription();
  }

  public void setShortDescription(String value) {
    template.setShortDescription(value);
  }

  public String getShortDescriptionEn() {
    return template.getShortDescriptionEn();
  }

  public void setShortDescriptionEn(String value) {
    template.setShortDescriptionEn(value);
  }

  public String getDescription() {
    return template.getDescription();
  }

  public void setDescription(String value) {
    template.setDescription(value);
  }

  public String getDescriptionEn() {
    return template.getDescriptionEn();
  }

  public void setDescriptionEn(String value) {
    template.setDescriptionEn(value);
  }

  public List<String> getHighlights() {
    return template.getHighlights();
  }

  public void setHighlights(List<String> value) {
    template.setHighlights(value);
  }

  public List<String> getHighlightsEn() {
    return template.getHighlightsEn();
  }

  public void setHighlightsEn(List<String> value) {
    template.setHighlightsEn(value);
  }

  public List<String> getCourseOutcomes() {
    return template.getCourseOutcomes();
  }

  public void setCourseOutcomes(List<String> value) {
    template.setCourseOutcomes(value);
  }

  public List<String> getCourseOutcomesEn() {
    return template.getCourseOutcomesEn();
  }

  public void setCourseOutcomesEn(List<String> value) {
    template.setCourseOutcomesEn(value);
  }

  public List<String> getRequirements() {
    return template.getRequirements();
  }

  public void setRequirements(List<String> value) {
    template.setRequirements(value);
  }

  public List<String> getRequirementsEn() {
    return template.getRequirementsEn();
  }

  public void setRequirementsEn(List<String> value) {
    template.setRequirementsEn(value);
  }

  public List<String> getPrerequisites() {
    return template.getPrerequisites();
  }

  public void setPrerequisites(List<String> value) {
    template.setPrerequisites(value);
  }

  public List<String> getPrerequisitesEn() {
    return template.getPrerequisitesEn();
  }

  public void setPrerequisitesEn(List<String> value) {
    template.setPrerequisitesEn(value);
  }

  public CourseLevel getLevel() {
    return template.getLevel();
  }

  public void setLevel(CourseLevel value) {
    template.setLevel(value);
  }

  public CourseLanguage getLanguage() {
    return template.getLanguage();
  }

  public void setLanguage(CourseLanguage value) {
    template.setLanguage(value);
  }

  public Integer getLiveSessionCount() {
    return template.getLiveSessionCount();
  }

  public void setLiveSessionCount(Integer value) {
    template.setLiveSessionCount(value);
  }

  public Integer getQuizCount() {
    return template.getQuizCount();
  }

  public void setQuizCount(Integer value) {
    template.setQuizCount(value);
  }

  public Integer getRecordedHoursCount() {
    return template.getRecordedHoursCount();
  }

  public void setRecordedHoursCount(Integer value) {
    template.setRecordedHoursCount(value);
  }

  public Lesson getPreviewLesson() {
    return template.getPreviewLesson();
  }

  public void setPreviewLesson(Lesson value) {
    template.setPreviewLesson(value);
  }

  public Integer getEstimatedDurationMinutes() {
    return template.getEstimatedDurationMinutes();
  }

  public void setEstimatedDurationMinutes(Integer value) {
    template.setEstimatedDurationMinutes(value);
  }

  public String getTargetAudience() {
    return template.getTargetAudience();
  }

  public void setTargetAudience(String value) {
    template.setTargetAudience(value);
  }

  public String getTargetAudienceEn() {
    return template.getTargetAudienceEn();
  }

  public void setTargetAudienceEn(String value) {
    template.setTargetAudienceEn(value);
  }
}
