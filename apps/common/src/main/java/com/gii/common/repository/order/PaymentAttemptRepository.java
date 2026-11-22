package com.gii.common.repository.order;

import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.PaymentAttempt;
import com.gii.common.enums.OrderProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

  Optional<PaymentAttempt> findTopByOrderIdAndProviderAndExpiresAtAfterOrderByCreatedAtDesc(
      UUID orderId, OrderProvider provider, Instant now);

  Optional<PaymentAttempt> findByProviderAndProviderTxnId(
      OrderProvider provider, String providerTxnId);

  Optional<PaymentAttempt> findTopByOrderIdAndProviderAndProviderTxnIdOrderByCreatedAtDesc(
      UUID orderId, OrderProvider provider, String providerTxnId);

  @Query(
      """
        SELECT a.order FROM PaymentAttempt a
        WHERE a.provider = :provider AND a.providerTxnId = :providerTxnId
      """)
  Optional<Order> findOrderByProviderAndProviderTxnId(
      @Param("provider") OrderProvider provider, @Param("providerTxnId") String providerTxnId);
}
