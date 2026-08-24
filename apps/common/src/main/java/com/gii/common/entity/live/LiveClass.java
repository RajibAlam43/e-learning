package com.gii.common.entity.live;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.entity.course.Course;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassProvisioningMode;
import com.gii.common.enums.LiveClassStatus;
import jakarta.persistence.CascadeType;
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
  @JoinColumn(name = "course_offering_id", nullable = false)
  private Course course;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.EAGER, optional = false, cascade = CascadeType.MERGE)
  @JoinColumn(name = "live_class_slot_id", nullable = false)
  private LiveClassSlot slot;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 30)
  private LiveClassProvider provider;

  @Column(name = "provider_meeting_id")
  private String providerMeetingId;

  @Column(name = "host_start_url")
  private String hostStartUrl;

  @Column(name = "participant_join_url")
  private String participantJoinUrl;

  @Column(name = "title_override")
  private String titleOverride;

  @Column(name = "title_en_override")
  private String titleEnOverride;

  @Column(name = "description_override")
  private String descriptionOverride;

  @Column(name = "description_en_override")
  private String descriptionEnOverride;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "provider_metadata", columnDefinition = "jsonb")
  private Map<String, Object> providerMetadata;

  @Enumerated(EnumType.STRING)
  @Column(name = "provisioning_mode", nullable = false, length = 30)
  @lombok.Builder.Default
  private LiveClassProvisioningMode provisioningMode = LiveClassProvisioningMode.API_PROVISIONED;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Column(name = "max_capacity")
  private Integer maxCapacity;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private LiveClassStatus status;

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

  public com.gii.common.entity.course.CourseSection getSection() {
    return slot.getSection();
  }

  public String getTitle() {
    return titleOverride != null ? titleOverride : slot.getTitle();
  }

  public void setTitle(String value) {
    titleOverride = value;
  }

  public String getTitleEn() {
    return titleEnOverride != null ? titleEnOverride : slot.getTitleEn();
  }

  public void setTitleEn(String value) {
    titleEnOverride = value;
  }

  public String getDescription() {
    return descriptionOverride != null ? descriptionOverride : slot.getDescription();
  }

  public void setDescription(String value) {
    descriptionOverride = value;
  }

  public String getDescriptionEn() {
    return descriptionEnOverride != null ? descriptionEnOverride : slot.getDescriptionEn();
  }

  public void setDescriptionEn(String value) {
    descriptionEnOverride = value;
  }
}
