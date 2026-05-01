package com.gii.common.entity.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.CreatedOnlyUuidEntity;
import com.gii.common.entity.user.User;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order extends CreatedOnlyUuidEntity {

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "amount_bdt", nullable = false)
  private BigDecimal amountBdt;

  @Column(name = "currency", nullable = false, length = 10)
  @Builder.Default
  private String currency = "BDT";

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 50)
  private OrderProvider provider;

  @Column(name = "provider_txn_id")
  private String providerTxnId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private OrderStatus status = OrderStatus.PENDING;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "refunded_at")
  private Instant refundedAt;
}
