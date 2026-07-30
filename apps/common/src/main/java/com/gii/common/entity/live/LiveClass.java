package com.gii.common.entity.live;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.user.User;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "live_classes")
public class LiveClass extends BaseUuidEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "section_id", nullable = false)
  private CourseSection section;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "instructor_id")
  private User instructor;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "title_en")
  private String titleEn;

  @Column(name = "description")
  private String description;

  @Column(name = "description_en")
  private String descriptionEn;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 30)
  private LiveClassProvider provider;

  @Column(name = "provider_meeting_id")
  private String providerMeetingId;

  @Column(name = "host_start_url")
  private String hostStartUrl;

  @Column(name = "participant_join_url")
  private String participantJoinUrl;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "provider_metadata", columnDefinition = "jsonb")
  private Map<String, Object> providerMetadata;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Column(name = "max_capacity")
  private Integer maxCapacity;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private LiveClassStatus status;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  /** Provider participant join URL. */
  public String effectiveParticipantJoinUrl() {
    return participantJoinUrl;
  }

  /** Provider host start URL. */
  public String effectiveHostStartUrl() {
    return hostStartUrl;
  }

  /** Provider meeting ID. */
  public String effectiveMeetingId() {
    return providerMeetingId;
  }
}
