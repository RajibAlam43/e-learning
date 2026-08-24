package com.gii.api.service.payment;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.order.OrderItemCourseRepository;
import com.gii.common.repository.order.OrderItemRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PendingOrderEligibilityService {

  private final OrderItemRepository orderItemRepository;
  private final OrderItemCourseRepository orderItemCourseRepository;
  private final CourseRepository courseRepository;
  private final OfferingEnrollmentPolicyService offeringEnrollmentPolicyService;

  @Transactional
  public void validate(Order order) {
    List<Course> courses = new ArrayList<>();
    for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
      if (item.getItemType() == OrderItemType.COURSE) {
        courses.add(item.getCourse());
        continue;
      }
      if (item.getCollection().getStatus() != PublishStatus.PUBLISHED) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Collection is not available");
      }
      List<Course> purchasedCourses =
          orderItemCourseRepository.findByOrderItemIdOrderByPositionAsc(item.getId()).stream()
              .map(snapshot -> snapshot.getCourse())
              .toList();
      if (purchasedCourses.isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Collection purchase has no course snapshot");
      }
      courses.addAll(purchasedCourses);
    }

    Instant now = Instant.now();
    courses.stream()
        .map(Course::getId)
        .distinct()
        .sorted(Comparator.naturalOrder())
        .map(this::lockCourse)
        .forEach(
            course ->
                offeringEnrollmentPolicyService.validateCheckout(
                    course, order.getUser().getId(), now));
  }

  private Course lockCourse(UUID courseId) {
    return courseRepository
        .findByIdForUpdate(courseId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
  }
}
