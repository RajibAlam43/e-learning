package com.gii.common.repository.order;

import com.gii.common.entity.order.Order;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

  List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

  List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);

  Optional<Order> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.id = :id")
  Optional<Order> findByIdForUpdate(@Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.id = :id AND o.user.id = :userId")
  Optional<Order> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

  Optional<Order> findByProviderAndProviderTxnId(OrderProvider provider, String providerTxnId);
}
