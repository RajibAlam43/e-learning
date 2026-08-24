package com.gii.common.repository.order;

import com.gii.common.entity.order.OrderItemCourse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemCourseRepository extends JpaRepository<OrderItemCourse, UUID> {

  List<OrderItemCourse> findByOrderItemIdOrderByPositionAsc(UUID orderItemId);
}
