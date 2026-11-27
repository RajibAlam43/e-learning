package com.gii.common.entity.academic;

import com.gii.common.entity.common.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "academic_semesters")
public class Semester extends BaseUuidEntity {
  @Column(nullable = false, unique = true, length = 40)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Column(name = "registration_starts_at", nullable = false)
  private Instant registrationStartsAt;

  @Column(name = "registration_ends_at", nullable = false)
  private Instant registrationEndsAt;
}
