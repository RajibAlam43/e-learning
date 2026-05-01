package com.gii.common.entity.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.CreatedOnlyUuidEntity;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.PaymentEventStatus;
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
import lombok.Builder;
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
@Table(name = "payment_events")
public class PaymentEvent extends CreatedOnlyUuidEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 50)
  private OrderProvider provider;

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "provider_event_id")
  private String providerEventId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_payload_json", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> rawPayloadJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private PaymentEventStatus status = PaymentEventStatus.RECEIVED;

  @Column(name = "processed_at")
  private Instant processedAt;
}
