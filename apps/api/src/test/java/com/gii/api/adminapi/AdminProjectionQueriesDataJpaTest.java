package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.PublishStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AdminProjectionQueriesDataJpaTest extends AbstractAdminDataJpaTest {

  @Autowired private com.gii.common.repository.enrollment.EnrollmentRepository enrollmentRepository;

  @Autowired
  private com.gii.common.repository.course.CourseInstructorRepository courseInstructorRepository;

  @Autowired
  private com.gii.common.repository.live.LiveClassRegistrantRepository
      liveClassRegistrantRepository;

  @Autowired private com.gii.common.repository.course.LessonRepository lessonRepository;

  @Test
  void enrollmentCountByCourseIdsAndStatusShouldReturnGroupedActiveCounts() {
    var creator = user("Creator Enrollment Cnt", "creator-enroll-cnt@example.com");
    var student1 = user("Student One", "student-one-enroll-cnt@example.com");
    var student2 = user("Student Two", "student-two-enroll-cnt@example.com");
    var student3 = user("Student Three", "student-three-enroll-cnt@example.com");
    var courseA = course("Course A", "course-a-enroll-cnt", creator);

    final var courseB = course("Course B", "course-b-enroll-cnt", creator);
    enrollment(student1, courseA, EnrollmentStatus.ACTIVE);
    enrollment(student2, courseA, EnrollmentStatus.ACTIVE);
    enrollment(student3, courseA, EnrollmentStatus.REVOKED);
    enrollment(student1, courseB, EnrollmentStatus.ACTIVE);

    List<Object[]> rows =
        enrollmentRepository.countByCourseIdsAndStatus(
            List.of(courseA.getId(), courseB.getId()), EnrollmentStatus.ACTIVE);
    Map<UUID, Long> counts =
        rows.stream().collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));

    assertThat(counts).containsEntry(courseA.getId(), 2L).containsEntry(courseB.getId(), 1L);
  }

  @Test
  void instructorCountQueriesShouldRespectCourseStatusFilter() {
    var creator = user("Creator Instructor Cnt", "creator-inst-cnt@example.com");
    var instructorA = user("Instructor A", "instructor-a-cnt@example.com");
    var instructorB = user("Instructor B", "instructor-b-cnt@example.com");
    instructorProfile(instructorA);
    instructorProfile(instructorB);

    var draftCourse = course("Draft Course", "draft-course-inst-cnt", creator, PublishStatus.DRAFT);
    var publishedCourse =
        course("Published Course", "published-course-inst-cnt", creator, PublishStatus.PUBLISHED);
    assignment(draftCourse, instructorA);
    assignment(publishedCourse, instructorA);
    assignment(publishedCourse, instructorB);

    List<Object[]> allRows =
        courseInstructorRepository.countByInstructorIds(
            List.of(instructorA.getId(), instructorB.getId()));
    Map<UUID, Long> allCounts =
        allRows.stream().collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));
    assertThat(allCounts)
        .containsEntry(instructorA.getId(), 2L)
        .containsEntry(instructorB.getId(), 1L);

    List<Object[]> publishedRows =
        courseInstructorRepository.countByInstructorIdsAndCourseStatus(
            List.of(instructorA.getId(), instructorB.getId()), PublishStatus.PUBLISHED);
    Map<UUID, Long> publishedCounts =
        publishedRows.stream().collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));
    assertThat(publishedCounts)
        .containsEntry(instructorA.getId(), 1L)
        .containsEntry(instructorB.getId(), 1L);
  }

  @Test
  void liveClassRegistrantAndLessonCountQueriesShouldReturnGroupedCounts() {
    var creator = user("Creator Live Cnt", "creator-live-cnt@example.com");
    var student1 = user("Student L1", "student-l1-live-cnt@example.com");
    var courseA = course("Course Live A", "course-live-a-cnt", creator);
    var courseB = course("Course Live B", "course-live-b-cnt", creator);
    var sectionA = section(courseA, 1);
    var sectionB = section(courseB, 1);
    final var student2 = user("Student L2", "student-l2-live-cnt@example.com");
    final var student3 = user("Student L3", "student-l3-live-cnt@example.com");
    var lessonA1 = lesson(courseA, sectionA, 1);
    lesson(courseA, sectionA, 2);
    lesson(courseB, sectionB, 1);

    var liveClass = liveClass(courseA, sectionA, lessonA1);
    registrant(liveClass, student1, LiveClassRegistrantStatus.APPROVED);
    registrant(liveClass, student2, LiveClassRegistrantStatus.APPROVED);
    registrant(liveClass, student3, LiveClassRegistrantStatus.PENDING);

    List<Object[]> registrantRows =
        liveClassRegistrantRepository.countByLiveClassIdsAndStatus(
            List.of(liveClass.getId()), LiveClassRegistrantStatus.APPROVED);
    Map<UUID, Long> approvedCounts =
        registrantRows.stream().collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));
    assertThat(approvedCounts).containsEntry(liveClass.getId(), 2L);

    List<Object[]> lessonRows =
        lessonRepository.countByCourseIdsAndStatus(
            List.of(courseA.getId(), courseB.getId()), PublishStatus.DRAFT);
    Map<UUID, Long> lessonCounts =
        lessonRows.stream().collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));
    assertThat(lessonCounts).containsEntry(courseA.getId(), 2L).containsEntry(courseB.getId(), 1L);
  }
}
