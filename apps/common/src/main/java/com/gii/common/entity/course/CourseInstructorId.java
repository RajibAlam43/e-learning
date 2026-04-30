package com.gii.common.entity.course;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Embeddable
public class CourseInstructorId implements Serializable {

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "instructor_user_id", nullable = false)
  private UUID instructorUserId;
}
