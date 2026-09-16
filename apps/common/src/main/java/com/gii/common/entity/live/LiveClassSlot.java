package com.gii.common.entity.live;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.entity.course.CourseSection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A live-class position in a course's curriculum. */
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "live_class_slots")
public class LiveClassSlot extends BaseUuidEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "section_id", nullable = false)
  private CourseSection section;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "title_en")
  private String titleEn;

  @Column(name = "description")
  private String description;

  @Column(name = "description_en")
  private String descriptionEn;

  @Column(name = "expected_duration_minutes")
  private Integer expectedDurationMinutes;

  @Column(name = "is_mandatory", nullable = false)
  private Boolean isMandatory;
}
