package com.gii.common.entity.course;

import com.gii.common.entity.common.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "course_templates")
public class CourseTemplate extends BaseUuidEntity {

  @Column(name = "internal_key", nullable = false, unique = true, length = 150)
  private String internalKey;
}
