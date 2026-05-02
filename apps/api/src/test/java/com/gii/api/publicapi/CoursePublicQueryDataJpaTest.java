package com.gii.api.publicapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CoursePublicQueryDataJpaTest extends AbstractPublicDataJpaTest {

  @Test
  void findBySlugAndStatusReturnsOnlyPublished() {
    User creator = user("Creator", "creator8@example.com", UserStatus.ACTIVE);
    Course published =
        course(
            "Published A",
            uniqueSlug("pub-a"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    course(
        "Draft A",
        uniqueSlug("draft-a"),
        PublishStatus.DRAFT,
        creator,
        CourseLevel.BEGINNER,
        CourseLanguage.EN,
        null);

    assertThat(courseRepository.findBySlugAndStatus(published.getSlug(), PublishStatus.PUBLISHED))
        .isPresent();
    assertThat(courseRepository.findBySlugAndStatus(published.getSlug(), PublishStatus.DRAFT))
        .isNotPresent();
  }

  @Test
  void sectionAndLessonQueriesReturnOnlyPublishedInPositionOrder() {
    User creator = user("Creator", "creator9@example.com", UserStatus.ACTIVE);
    Course course =
        course(
            "Course",
            uniqueSlug("course"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    CourseSection sec2 = section(course, uniqueSlug("sec2"), 2, PublishStatus.PUBLISHED);
    CourseSection sec1 = section(course, uniqueSlug("sec1"), 1, PublishStatus.PUBLISHED);
    section(course, uniqueSlug("sec3"), 3, PublishStatus.DRAFT);
    lesson(course, sec2, uniqueSlug("l2"), 2, PublishStatus.PUBLISHED, false);
    lesson(course, sec1, uniqueSlug("l1"), 1, PublishStatus.PUBLISHED, false);
    lesson(course, sec1, uniqueSlug("l3"), 3, PublishStatus.DRAFT, false);

    var sections =
        courseSectionRepository.findByCourseIdAndStatusOrderByPositionAsc(
            course.getId(), PublishStatus.PUBLISHED);
    var lessons =
        lessonRepository.findByCourseIdAndStatusWithMediaOrderByPositionAsc(
            course.getId(), PublishStatus.PUBLISHED);

    assertThat(sections).hasSize(2);
    assertThat(sections.get(0).getPosition()).isEqualTo(1);
    assertThat(sections.get(1).getPosition()).isEqualTo(2);
    assertThat(lessons).hasSize(2);
    assertThat(lessons.get(0).getPosition()).isEqualTo(1);
    assertThat(lessons.get(1).getPosition()).isEqualTo(2);
  }

  @Test
  void categoryAndInstructorBatchQueriesReturnJoinedRows() {
    User creator = user("Creator", "creator12@example.com", UserStatus.ACTIVE);
    User instructor = user("Instructor", "ins12@example.com", UserStatus.ACTIVE);
    Course course =
        course(
            "Course R",
            uniqueSlug("course-r"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    var category = category("Programming", uniqueSlug("programming-r"));
    attachCategory(course, category);
    attachInstructor(course, instructor);

    var categories = courseCategoryRepository.findByCourseIds(java.util.List.of(course.getId()));
    var instructors = courseInstructorRepository.findByCourseIds(java.util.List.of(course.getId()));

    assertThat(categories).hasSize(1);
    assertThat(categories.getFirst().getCategory().getName()).isEqualTo("Programming");
    assertThat(instructors).hasSize(1);
    assertThat(instructors.getFirst().getInstructor().getFullName()).isEqualTo("Instructor");
  }
}
