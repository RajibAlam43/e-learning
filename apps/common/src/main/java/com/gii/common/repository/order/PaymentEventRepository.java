package com.gii.common.repository.order;

import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.PaymentEventStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

  List<PaymentEvent> findByOrderId(UUID orderId);

  Optional<PaymentEvent> findByProviderAndProviderEventId(
      OrderProvider provider, String providerEventId);

  List<PaymentEvent> findByStatus(PaymentEventStatus status);
}
