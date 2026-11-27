package com.gii.common.entity.faculty;

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
@Table(name = "faculty_groups")
public class FacultyGroup extends BaseUuidEntity {
  @Column(nullable = false)
  private String name;

  private String nameEn;

  @Column(nullable = false)
  private Integer position;

  @Column(nullable = false)
  private Boolean visible;
}
