package com.gii.common.repository.order;

import com.gii.common.model.enums.OrderProvider;
import com.gii.common.model.order.Order;
import com.gii.common.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);

    Optional<Order> findByProviderAndProviderTxnId(OrderProvider provider, String providerTxnId);

    long countByStatus(OrderStatus status);
}