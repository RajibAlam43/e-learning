package com.gii.common.model.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.model.user.User;
import com.gii.common.model.common.CreatedOnlyUuidEntity;
import com.gii.common.model.enums.OrderProvider;
import com.gii.common.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order extends CreatedOnlyUuidEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount_bdt", nullable = false)
    private Integer amountBdt;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "BDT";

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private OrderProvider provider;

    @Column(name = "provider_txn_id")
    private String providerTxnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.pending;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;
}