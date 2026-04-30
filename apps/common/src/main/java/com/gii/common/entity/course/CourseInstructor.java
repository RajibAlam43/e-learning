package com.gii.common.entity.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.user.User;
import com.gii.common.enums.InstructorRole;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "course_instructors")
public class CourseInstructor {

  @EmbeddedId @Builder.Default private CourseInstructorId id = CourseInstructorId.builder().build();

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("courseId")
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("instructorUserId")
  @JoinColumn(name = "instructor_user_id", nullable = false)
  private User instructor;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 50)
  @Builder.Default
  private InstructorRole role = InstructorRole.PRIMARY;
}
