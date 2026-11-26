package com.gii.common.service.payment;

import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemCourseRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaidOrderEnrollmentService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderItemCourseRepository orderItemCourseRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CollectionEnrollmentRepository collectionEnrollmentRepository;
  private final CollectionCourseRepository collectionCourseRepository;
  private final CourseRepository courseRepository;
  private final CourseEnrollmentPolicyService enrollmentPolicyService;
  private final EnrollmentMutationLockService mutationLock;
  private final ProgramCourseAccessService programAccess;

  @Transactional
  public void grant(UUID orderId) {
    Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
    if (order.getStatus() != OrderStatus.PAID) {
      return;
    }
    mutationLock.lock(order.getUser().getId());
    Instant now = Instant.now();
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    Map<UUID, Course> purchasedCourseById = new LinkedHashMap<>();
    for (OrderItem item : items) {
      if (item.getItemType() == OrderItemType.COURSE) {
        purchasedCourseById.put(item.getCourse().getId(), item.getCourse());
      } else if (item.getItemType() == OrderItemType.COLLECTION && !deferredProgram(item)) {
        purchasedCourses(item).forEach(course -> purchasedCourseById.put(course.getId(), course));
      }
    }
    purchasedCourseById.keySet().stream()
        .sorted()
        .forEach(courseId -> courseRepository.findByIdForUpdate(courseId).orElseThrow());

    for (OrderItem item : items) {
      if (item.getItemType() == OrderItemType.COURSE) {
        activateOrCreateCourseEnrollment(order, item, item.getCourse(), now, item.getCollection());
      } else if (item.getItemType() == OrderItemType.COLLECTION) {
        activateOrCreateCollectionEnrollment(order, item, now);
        if (deferredProgram(item)) {
          continue;
        }
        purchasedCourses(item)
            .forEach(course ->
                activateOrCreateCourseEnrollment(order, item, course, now, item.getCollection()));
      }
    }
  }

  private boolean deferredProgram(OrderItem item) {
    return item.getProgramSnapshot() != null && item.getProgramSnapshot().creditBased();
  }

  private List<Course> purchasedCourses(OrderItem item) {
    List<Course> snapshot =
        orderItemCourseRepository.findByOrderItemIdOrderByPositionAsc(item.getId()).stream()
            .map(row -> row.getCourse())
            .toList();
    if (!snapshot.isEmpty()) {
      return snapshot;
    }
    return collectionCourseRepository
        .findByCollection_IdOrderByPositionAsc(item.getCollection().getId())
        .stream()
        .map(row -> row.getCourse())
        .toList();
  }

  private void activateOrCreateCourseEnrollment(
      Order order,
      OrderItem sourceOrderItem,
      Course course,
      Instant now,
      com.gii.common.entity.collection.Collection sourceCollection) {
    Course lockedCourse = courseRepository.findByIdForUpdate(course.getId()).orElseThrow();
    Enrollment existing =
        enrollmentRepository
            .findByUserIdAndCourseId(order.getUser().getId(), course.getId())
            .orElse(null);
    if (existing != null) {
      boolean alreadyOccupiesSeat =
          existing.getStatus() == EnrollmentStatus.ACTIVE
              && (existing.getExpiresAt() == null || existing.getExpiresAt().isAfter(now));
      if (alreadyOccupiesSeat) {
        if (existing.isProgramAccessManaged()) {
          existing.setSourceOrderItem(sourceOrderItem);
          existing.setSourceCollection(sourceCollection);
          programAccess.independentGrant(
              existing, enrollmentPolicyService.calculateExpiry(lockedCourse, now));
        }
        return;
      }
      enrollmentPolicyService.validateActivation(
          lockedCourse, order.getUser().getId(), order.getId(), order.getCreatedAt(), now);
      existing.setStatus(EnrollmentStatus.ACTIVE);
      existing.setEnrolledAt(now);
      existing.setRevokedAt(null);
      existing.setExpiresAt(enrollmentPolicyService.calculateExpiry(lockedCourse, now));
      existing.setSourceOrderItem(sourceOrderItem);
      existing.setSourceCollection(sourceCollection);
      if (existing.isProgramAccessManaged()) {
        programAccess.independentGrant(existing, existing.getExpiresAt());
      }
      enrollmentRepository.save(existing);
      return;
    }

    enrollmentPolicyService.validateActivation(
        lockedCourse, order.getUser().getId(), order.getId(), order.getCreatedAt(), now);
    enrollmentRepository.saveAndFlush(
        Enrollment.builder()
            .user(order.getUser())
            .course(lockedCourse)
            .sourceOrderItem(sourceOrderItem)
            .sourceCollection(sourceCollection)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(now)
            .expiresAt(enrollmentPolicyService.calculateExpiry(lockedCourse, now))
            .build());
  }

  private void activateOrCreateCollectionEnrollment(
      Order order, OrderItem sourceOrderItem, Instant now) {
    CollectionEnrollment existing =
        collectionEnrollmentRepository
            .findByUserIdAndCollectionId(
                order.getUser().getId(), sourceOrderItem.getCollection().getId())
            .orElse(null);
    if (existing != null) {
      // Retries must not renew access or replace the original academic contract.
      if (existing.getSourceOrderItem() != null
          && existing.getSourceOrderItem().getId().equals(sourceOrderItem.getId())) {
        return;
      }
      if (existing.getProgramSnapshot() != null
          && existing.getStatus() == EnrollmentStatus.ACTIVE
          && (existing.getExpiresAt() == null || existing.getExpiresAt().isAfter(now))) {
        return;
      }
      existing.setStatus(EnrollmentStatus.ACTIVE);
      existing.setEnrolledAt(deferredProgram(sourceOrderItem) ? order.getPaidAt() : now);
      existing.setRevokedAt(null);
      existing.setExpiresAt(ProgramSnapshotService.expiresAt(sourceOrderItem));
      if (existing.getProgramSnapshot() == null) {
        existing.setProgramSnapshot(sourceOrderItem.getProgramSnapshot());
      }
      existing.setSourceOrderItem(sourceOrderItem);
      collectionEnrollmentRepository.save(existing);
      return;
    }
    collectionEnrollmentRepository.saveAndFlush(
        CollectionEnrollment.builder()
            .user(order.getUser())
            .collection(sourceOrderItem.getCollection())
            .programSnapshot(sourceOrderItem.getProgramSnapshot())
            .expiresAt(ProgramSnapshotService.expiresAt(sourceOrderItem))
            .sourceOrderItem(sourceOrderItem)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(deferredProgram(sourceOrderItem) ? order.getPaidAt() : now)
            .build());
  }
}
