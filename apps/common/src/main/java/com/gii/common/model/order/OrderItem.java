package com.gii.common.model.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.model.course.Course;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "price_bdt", nullable = false)
    private Integer priceBdt;

    @Column(name = "discount_bdt", nullable = false)
    private Integer discountBdt = 0;
}