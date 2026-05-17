package com.gii.common.entity.collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.course.Course;
import jakarta.persistence.Column;
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
@Table(name = "collection_courses")
public class CollectionCourse {

  @EmbeddedId @Builder.Default private CollectionCourseId id = new CollectionCourseId();

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("collectionId")
  @JoinColumn(name = "collection_id", nullable = false)
  private Collection collection;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("courseId")
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(name = "position", nullable = false)
  private Integer position;

  @Column(name = "is_mandatory", nullable = false)
  @Builder.Default
  private Boolean isMandatory = true;
}
