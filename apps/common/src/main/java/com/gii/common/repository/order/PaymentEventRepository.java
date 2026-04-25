package com.gii.common.repository.order;

import com.gii.common.model.order.PaymentEvent;
import com.gii.common.model.enums.PaymentEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    List<PaymentEvent> findByOrderId(UUID orderId);

    Optional<PaymentEvent> findByProviderEventId(String providerEventId);

    List<PaymentEvent> findByStatus(PaymentEventStatus status);
}