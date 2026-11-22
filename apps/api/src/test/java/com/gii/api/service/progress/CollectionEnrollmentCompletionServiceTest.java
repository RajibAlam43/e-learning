package com.gii.api.service.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gii.api.service.collection.PurchasedCollectionCoursesService;
import com.gii.api.testsupport.CourseTestData;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectionEnrollmentCompletionServiceTest {

  @Mock private CollectionEnrollmentRepository collectionEnrollmentRepository;
  @Mock private CollectionCourseRepository collectionCourseRepository;
  @Mock private CourseCompletionService courseCompletionService;
  @Mock private PurchasedCollectionCoursesService purchasedCollectionCoursesService;

  @InjectMocks private CollectionEnrollmentCompletionService service;

  @Test
  void refreshSetsCompletedAtWhenEveryMandatoryCourseIsComplete() {
    UUID userId = UUID.randomUUID();
    UUID collectionId = UUID.randomUUID();
    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(userId);
    Course courseA = CourseTestData.course("Course A", "course-a", user);
    courseA.setId(UUID.randomUUID());
    Collection collection = Collection.builder().title("Pack").slug("pack").type(CollectionType.PACK).build();
    collection.setId(collectionId);
    CollectionEnrollment enrollment =
        CollectionEnrollment.builder()
            .user(user)
            .collection(collection)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now().minusSeconds(1000))
            .build();

    when(collectionEnrollmentRepository.findByUserIdAndCollectionIdForUpdate(userId, collectionId))
        .thenReturn(Optional.of(enrollment));
    when(purchasedCollectionCoursesService.resolveItems(enrollment))
        .thenReturn(List.of(new PurchasedCollectionCoursesService.PurchasedCourse(courseA, true)));
    when(courseCompletionService.getByCourseIds(userId, List.of(courseA.getId())))
        .thenReturn(
            java.util.Map.of(
                courseA.getId(),
                CourseCompletionService.CourseCompletion.create(3, 3, 1, 1, 0, 0, false)));

    service.refresh(userId, collectionId);

    ArgumentCaptor<CollectionEnrollment> captor = ArgumentCaptor.forClass(CollectionEnrollment.class);
    verify(collectionEnrollmentRepository).save(captor.capture());
    assertThat(captor.getValue().getCompletedAt()).isNotNull();
  }

  @Test
  void refreshIsNoOpWhenAlreadyCompleted() {
    UUID userId = UUID.randomUUID();
    UUID collectionId = UUID.randomUUID();
    User user = User.builder().fullName("Student").email("student@example.com").build();
    user.setId(userId);
    Collection collection = Collection.builder().title("Pack").slug("pack").type(CollectionType.PACK).build();
    collection.setId(collectionId);
    CollectionEnrollment enrollment =
        CollectionEnrollment.builder()
            .user(user)
            .collection(collection)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now().minusSeconds(1000))
            .completedAt(Instant.now().minusSeconds(500))
            .build();

    when(collectionEnrollmentRepository.findByUserIdAndCollectionIdForUpdate(userId, collectionId))
        .thenReturn(Optional.of(enrollment));

    service.refresh(userId, collectionId);

    verify(collectionEnrollmentRepository, never()).save(any());
  }

  @Test
  void refreshCollectionsForCourseOnlyRefreshesActiveEnrollments() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID activeCollectionId = UUID.randomUUID();
    UUID inactiveCollectionId = UUID.randomUUID();

    Collection activeCollection =
        Collection.builder().title("Active Pack").slug("active-pack").type(CollectionType.PACK).build();
    activeCollection.setId(activeCollectionId);
    Collection inactiveCollection =
        Collection.builder()
            .title("Inactive Pack")
            .slug("inactive-pack")
            .type(CollectionType.PACK)
            .build();
    inactiveCollection.setId(inactiveCollectionId);

    CollectionCourse activeMembership =
        CollectionCourse.builder().collection(activeCollection).build();
    CollectionCourse inactiveMembership =
        CollectionCourse.builder().collection(inactiveCollection).build();

    when(collectionCourseRepository.findByCourse_Id(courseId))
        .thenReturn(List.of(activeMembership, inactiveMembership));
    when(collectionEnrollmentRepository.existsByUserIdAndCollectionIdAndStatus(
            userId, activeCollectionId, EnrollmentStatus.ACTIVE))
        .thenReturn(true);
    when(collectionEnrollmentRepository.existsByUserIdAndCollectionIdAndStatus(
            userId, inactiveCollectionId, EnrollmentStatus.ACTIVE))
        .thenReturn(false);
    when(collectionEnrollmentRepository.findByUserIdAndCollectionIdForUpdate(userId, activeCollectionId))
        .thenReturn(Optional.empty());

    service.refreshCollectionsForCourse(userId, courseId);

    verify(collectionEnrollmentRepository)
        .findByUserIdAndCollectionIdForUpdate(userId, activeCollectionId);
    verify(collectionEnrollmentRepository, never())
        .findByUserIdAndCollectionIdForUpdate(userId, inactiveCollectionId);
  }
}
