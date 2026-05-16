package com.gii.api.paymentapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gii.common.entity.order.PaymentEvent;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PaymentEventStatus;
import com.gii.common.enums.PaymentEventType;
import com.gii.common.enums.PublishStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class PaymentRepositoriesDataJpaTest extends AbstractPaymentDataJpaTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void orderRepositoryLookupsShouldReturnOwnedStatusAndProviderTxnMatches() {
    var student = user("Student Repo", "student-payment-repo@example.com");
    var creator = user("Creator Repo", "creator-payment-repo@example.com");
    var course =
        course(
            "Repo Payment Course",
            "repo-payment-course",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1000));
    var pending =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-repo-pending",
            BigDecimal.valueOf(1000));
    var paid =
        order(
            student,
            OrderStatus.PAID,
            OrderProvider.BKASH,
            "txn-repo-paid",
            BigDecimal.valueOf(1000));
    orderItem(pending, course, BigDecimal.valueOf(1000), BigDecimal.ZERO);
    orderItem(paid, course, BigDecimal.valueOf(1000), BigDecimal.ZERO);

    assertThat(orderRepository.findByUserIdAndStatus(student.getId(), OrderStatus.PENDING))
        .extracting("id")
        .containsExactly(pending.getId());
    assertThat(orderRepository.findByIdAndUserId(paid.getId(), student.getId())).isPresent();
    assertThat(orderRepository.findByProviderAndProviderTxnId(OrderProvider.BKASH, "txn-repo-paid"))
        .isPresent();
  }

  @Test
  void paymentEventRepositoryShouldEnforceUniqueProviderEventIdPerProvider() {
    var student = user("Student Event", "student-payment-event@example.com");
    var creator = user("Creator Event", "creator-payment-event@example.com");
    var course =
        course(
            "Event Payment Course",
            "event-payment-course",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1000));
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-event",
            BigDecimal.valueOf(1000));
    orderItem(order, course, BigDecimal.valueOf(1000), BigDecimal.ZERO);

    paymentEvent(order, OrderProvider.SSLCOMMERZ, "evt-uniq-1", PaymentEventStatus.RECEIVED);
    assertThat(
            paymentEventRepository.findByProviderAndProviderEventId(
                OrderProvider.SSLCOMMERZ, "evt-uniq-1"))
        .isPresent();

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            paymentEventRepository.saveAndFlush(
                PaymentEvent.builder()
                    .order(order)
                    .provider(OrderProvider.SSLCOMMERZ)
                    .eventType(PaymentEventType.SSLCOMMERZ_WEBHOOK)
                    .providerEventId("evt-uniq-1")
                    .rawPayloadJson(java.util.Map.of("k", "v2"))
                    .status(PaymentEventStatus.PROCESSED)
                    .processedAt(Instant.now())
                    .build()));
  }

  @Test
  void paymentEventRepositoryFindByStatusShouldReturnMatchingEvents() {
    var student = user("Student Status", "student-payment-status-events@example.com");
    var creator = user("Creator Status", "creator-payment-status-events@example.com");
    var course =
        course(
            "Status Event Course",
            "status-event-course-payment",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1000));
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-status-events",
            BigDecimal.valueOf(1000));
    orderItem(order, course, BigDecimal.valueOf(1000), BigDecimal.ZERO);

    paymentEvent(order, OrderProvider.SSLCOMMERZ, "evt-status-rec-1", PaymentEventStatus.RECEIVED);
    paymentEvent(
        order, OrderProvider.SSLCOMMERZ, "evt-status-proc-1", PaymentEventStatus.PROCESSED);

    assertThat(paymentEventRepository.findByStatus(PaymentEventStatus.RECEIVED))
        .extracting(PaymentEvent::getProviderEventId)
        .contains("evt-status-rec-1");
    assertThat(paymentEventRepository.findByStatus(PaymentEventStatus.PROCESSED))
        .extracting(PaymentEvent::getProviderEventId)
        .contains("evt-status-proc-1");
  }

  @Test
  void orderItemPricingShouldBeImmutableAtDatabaseLevel() {
    Integer triggerCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_trigger t
            JOIN pg_class c ON c.oid = t.tgrelid
            WHERE t.tgname = 'trg_order_items_pricing_immutable'
              AND c.relname = 'order_items'
              AND NOT t.tgisinternal
            """,
            Integer.class);
    assertThat(triggerCount).isEqualTo(1);

    var student = user("Student Immutable", "student-payment-immutable@example.com");
    var creator = user("Creator Immutable", "creator-payment-immutable@example.com");
    var course =
        course(
            "Immutable Price Course",
            "immutable-price-course",
            creator,
            PublishStatus.PUBLISHED,
            BigDecimal.valueOf(1000));
    var order =
        order(
            student,
            OrderStatus.PENDING,
            OrderProvider.SSLCOMMERZ,
            "txn-immutable-price",
            BigDecimal.valueOf(1000));
    var item = orderItem(order, course, BigDecimal.valueOf(1000), BigDecimal.valueOf(50));
    orderItemRepository.flush();

    assertThrows(
        DataAccessException.class,
        () ->
            jdbcTemplate.update(
                "UPDATE order_items SET price_bdt = ? WHERE id = ?",
                BigDecimal.valueOf(999),
                item.getId()));
    assertThrows(
        DataAccessException.class,
        () ->
            jdbcTemplate.update(
                "UPDATE order_items SET discount_bdt = ? WHERE id = ?",
                BigDecimal.valueOf(60),
                item.getId()));
  }
}
