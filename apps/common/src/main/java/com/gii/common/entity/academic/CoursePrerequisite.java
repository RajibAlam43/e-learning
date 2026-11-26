package com.gii.common.entity.academic;

import com.gii.common.entity.common.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "course_prerequisite_links")
public class CoursePrerequisite extends BaseUuidEntity {
  @Column(nullable = false)
  private UUID courseTemplateId;

  @Column(nullable = false)
  private UUID prerequisiteTemplateId;
}
