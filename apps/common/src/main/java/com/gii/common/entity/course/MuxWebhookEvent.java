package com.gii.common.entity.course;

import com.gii.common.entity.common.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
    name = "mux_webhook_events",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_mux_webhook_event_id", columnNames = "event_id")
    })
public class MuxWebhookEvent extends BaseUuidEntity {

  @Column(name = "event_id", nullable = false)
  private String eventId;

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;
}
