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
  private final com.gii.common.repository.course.CourseRepository courseRepository;
  private final com.gii.common.repository.academic.ProgramCourseRegistrationRepository
      registrations;

  public List<Course> resolve(CollectionEnrollment enrollment) {
    return resolveItems(enrollment).stream().map(PurchasedCourse::course).toList();
  }

  public List<PurchasedCourse> resolveItems(CollectionEnrollment enrollment) {
    java.util.Map<java.util.UUID, PurchasedCourse> byTemplate = new java.util.LinkedHashMap<>();
    for (var original : resolveRequiredItems(enrollment)) {
      byTemplate.put(original.course().getTemplate().getId(), original);
    }
    for (var row :
        collectionCourseRepository.findByCollection_IdOrderByPositionAsc(
            enrollment.getCollection().getId())) {
      byTemplate.putIfAbsent(
          row.getCourse().getTemplate().getId(), new PurchasedCourse(row.getCourse(), false));
    }
    for (var r :
        registrations.findByProgramEnrollmentIdAndStatus(
            enrollment.getId(), com.gii.common.enums.ProgramRegistrationStatus.ACTIVE)) {
      byTemplate.putIfAbsent(
          r.getCourseTemplateId(),
          new PurchasedCourse(r.getEnrollment().getCourse(), r.isMandatory()));
    }
    return List.copyOf(byTemplate.values());
  }

  public List<PurchasedCourse> resolveRequiredItems(CollectionEnrollment enrollment) {
    if (enrollment.getProgramSnapshot() != null) {
      var snapshot = enrollment.getProgramSnapshot();
      var courses =
          courseRepository.findAllById(snapshot.courses().stream().map(c -> c.courseId()).toList());
      var byId = courses.stream().collect(java.util.stream.Collectors.toMap(Course::getId, c -> c));
      return snapshot.courses().stream()
          .filter(c -> byId.containsKey(c.courseId()))
          .map(c -> new PurchasedCourse(byId.get(c.courseId()), c.mandatory()))
          .toList();
    }
    if (enrollment.getSourceOrderItem() != null) {
      List<PurchasedCourse> snapshot =
          orderItemCourseRepository
              .findByOrderItemIdOrderByPositionAsc(enrollment.getSourceOrderItem().getId())
              .stream()
              .map(row ->
                  new PurchasedCourse(row.getCourse(), !Boolean.FALSE.equals(row.getIsMandatory())))
              .toList();
      if (!snapshot.isEmpty()) {
        return snapshot;
      }
    }
    return collectionCourseRepository
        .findByCollection_IdOrderByPositionAsc(enrollment.getCollection().getId())
        .stream()
        .map(row ->
            new PurchasedCourse(row.getCourse(), !Boolean.FALSE.equals(row.getIsMandatory())))
        .toList();
  }

  public record PurchasedCourse(Course course, boolean mandatory) {}
}
