package com.gii.api.model.request.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCourseRequest {
  private String title;
  private String titleEn;
  private String slug;

  @Size(min = 1)
  private List<@NotNull UUID> categoryIds;

  private String thumbnailObjectKey;

  @Valid
  @Setter(AccessLevel.NONE)
  private CourseVideoRequest video;

  private String shortDescription;
  private String shortDescriptionEn;
  private String description;
  private String descriptionEn;
  private List<String> highlights;
  private List<String> highlightsEn;
  private BigDecimal priceBdt;
  private List<String> courseOutcomes;
  private List<String> courseOutcomesEn;
  private List<String> requirements;
  private List<String> requirementsEn;
  private String level;
  private String language;
  private String studyMode;
  private Boolean isFree;
  private Integer estimatedDurationMinutes;
  private String targetAudience;
  private String targetAudienceEn;
  private String prerequisites;
  private String prerequisitesEn;

  @Size(max = 80)
  @Setter(AccessLevel.NONE)
  private String timezone;

  @Setter(AccessLevel.NONE)
  private Instant enrollmentStartsAt;

  @Setter(AccessLevel.NONE)
  private Instant enrollmentEndsAt;

  @Setter(AccessLevel.NONE)
  private Instant startsAt;

  @Setter(AccessLevel.NONE)
  private Instant endsAt;

  @Positive
  @Setter(AccessLevel.NONE)
  private Integer capacity;

  @Positive
  @Setter(AccessLevel.NONE)
  private Integer accessDurationDays;

  @Setter(AccessLevel.NONE)
  @Getter(AccessLevel.NONE)
  private boolean timezonePresent;

  @Setter(AccessLevel.NONE)
  @Getter(AccessLevel.NONE)
  private boolean enrollmentStartsAtPresent;

  @Setter(AccessLevel.NONE)
  @Getter(AccessLevel.NONE)
  private boolean enrollmentEndsAtPresent;

  @Setter(AccessLevel.NONE)
  @Getter(AccessLevel.NONE)
  private boolean startsAtPresent;

  @Setter(AccessLevel.NONE)
  @Getter(AccessLevel.NONE)
  private boolean endsAtPresent;

  @Setter(AccessLevel.NONE)
  @Getter(AccessLevel.NONE)
  private boolean capacityPresent;

  @Setter(AccessLevel.NONE)
  @Getter(AccessLevel.NONE)
  private boolean accessDurationDaysPresent;

  @Setter(AccessLevel.NONE)
  @Getter(AccessLevel.NONE)
  private boolean videoPresent;

  @JsonIgnore
  public boolean isTimezonePresent() {
    return timezonePresent;
  }

  @JsonIgnore
  public boolean isEnrollmentStartsAtPresent() {
    return enrollmentStartsAtPresent;
  }

  @JsonIgnore
  public boolean isEnrollmentEndsAtPresent() {
    return enrollmentEndsAtPresent;
  }

  @JsonIgnore
  public boolean isStartsAtPresent() {
    return startsAtPresent;
  }

  @JsonIgnore
  public boolean isEndsAtPresent() {
    return endsAtPresent;
  }

  @JsonIgnore
  public boolean isCapacityPresent() {
    return capacityPresent;
  }

  @JsonIgnore
  public boolean isAccessDurationDaysPresent() {
    return accessDurationDaysPresent;
  }

  @JsonIgnore
  public boolean isVideoPresent() {
    return videoPresent;
  }

  @JsonSetter("video")
  public void setVideoValue(CourseVideoRequest value) {
    video = value;
    videoPresent = true;
  }

  @JsonSetter("timezone")
  public void setTimezoneValue(String value) {
    timezone = value;
    timezonePresent = true;
  }

  @JsonSetter("enrollmentStartsAt")
  public void setEnrollmentStartsAtValue(Instant value) {
    enrollmentStartsAt = value;
    enrollmentStartsAtPresent = true;
  }

  @JsonSetter("enrollmentEndsAt")
  public void setEnrollmentEndsAtValue(Instant value) {
    enrollmentEndsAt = value;
    enrollmentEndsAtPresent = true;
  }

  @JsonSetter("startsAt")
  public void setStartsAtValue(Instant value) {
    startsAt = value;
    startsAtPresent = true;
  }

  @JsonSetter("endsAt")
  public void setEndsAtValue(Instant value) {
    endsAt = value;
    endsAtPresent = true;
  }

  @JsonSetter("capacity")
  public void setCapacityValue(Integer value) {
    capacity = value;
    capacityPresent = true;
  }

  @JsonSetter("accessDurationDays")
  public void setAccessDurationDaysValue(Integer value) {
    accessDurationDays = value;
    accessDurationDaysPresent = true;
  }
}
