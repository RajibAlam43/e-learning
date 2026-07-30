package com.gii.common.entity.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.course.Course;
import com.gii.common.enums.OrderItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private java.util.UUID id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id")
  private Course course;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "collection_id")
  private Collection collection;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type", nullable = false, length = 20)
  private OrderItemType itemType;

  @Column(name = "title_snapshot", nullable = false)
  private String titleSnapshot;

  @Column(name = "title_snapshot_en")
  private String titleSnapshotEn;

  @Column(name = "price_bdt", nullable = false, updatable = false)
  private BigDecimal priceBdt;

  @Column(name = "discount_bdt", nullable = false, updatable = false)
  @Builder.Default
  private BigDecimal discountBdt = BigDecimal.ZERO;
}
