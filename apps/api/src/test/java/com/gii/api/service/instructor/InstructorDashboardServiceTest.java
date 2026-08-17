package com.gii.api.service.instructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstructorDashboardServiceTest {

  @Test
  void reviewAggregationPreservesCountsAboveIntegerRange() {
    long largeCount = (long) Integer.MAX_VALUE + 25L;
    var aggregate =
        InstructorDashboardService.aggregateReviewRows(
            List.of(
                new Object[] {UUID.randomUUID(), 4.0, BigInteger.valueOf(largeCount)},
                new Object[] {UUID.randomUUID(), 2.0, 25L}));

    assertThat(aggregate.totalReviews()).isEqualTo(largeCount + 25L);
    assertThat(aggregate.averageRating())
        .isCloseTo(
            ((4.0 * largeCount) + (2.0 * 25L)) / (largeCount + 25L),
            org.assertj.core.data.Offset.offset(0.000000001));
  }

  @Test
  void countMapAcceptsAnyJpaNumberWithoutNarrowing() {
    UUID courseId = UUID.randomUUID();
    long largeCount = (long) Integer.MAX_VALUE + 1L;

    var counts =
        InstructorDashboardService.toCountMap(
            List.<Object[]>of(new Object[] {courseId, BigInteger.valueOf(largeCount)}));

    assertThat(counts).containsEntry(courseId, largeCount);
  }

  @Test
  void reviewAggregationFailsExplicitlyIfLongCapacityIsExceeded() {
    List<Object[]> rows =
        List.of(
            new Object[] {UUID.randomUUID(), 5.0, Long.MAX_VALUE},
            new Object[] {UUID.randomUUID(), 5.0, 1L});

    assertThatThrownBy(() -> InstructorDashboardService.aggregateReviewRows(rows))
        .isInstanceOf(ArithmeticException.class);
  }
}
