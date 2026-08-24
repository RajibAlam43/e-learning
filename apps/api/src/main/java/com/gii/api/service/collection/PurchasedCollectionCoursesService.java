package com.gii.api.service.collection;

import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.order.OrderItemCourseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchasedCollectionCoursesService {

  private final OrderItemCourseRepository orderItemCourseRepository;
  private final CollectionCourseRepository collectionCourseRepository;

  public List<Course> resolve(CollectionEnrollment enrollment) {
    if (enrollment.getSourceOrderItem() != null) {
      List<Course> snapshot =
          orderItemCourseRepository
              .findByOrderItemIdOrderByPositionAsc(enrollment.getSourceOrderItem().getId())
              .stream()
              .map(row -> row.getCourse())
              .toList();
      if (!snapshot.isEmpty()) {
        return snapshot;
      }
    }
    return collectionCourseRepository
        .findByCollection_IdOrderByPositionAsc(enrollment.getCollection().getId())
        .stream()
        .map(row -> row.getCourse())
        .toList();
  }
}
