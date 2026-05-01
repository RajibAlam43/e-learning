package com.gii.common.entity.course;

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
@Table(name = "course_categories")
public class CourseCategory {

  @EmbeddedId @Builder.Default private CourseCategoryId id = new CourseCategoryId();

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("courseId")
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("categoryId")
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;
}
