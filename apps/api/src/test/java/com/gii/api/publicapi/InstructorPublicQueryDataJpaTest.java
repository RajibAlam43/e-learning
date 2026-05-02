package com.gii.api.publicapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InstructorPublicQueryDataJpaTest extends AbstractPublicDataJpaTest {

  @Test
  void findPublicProfilesByUserStatusFiltersCorrectly() {
    User active = user("Active Instructor", "ins-active@example.com", UserStatus.ACTIVE);
    User suspended =
        user("Suspended Instructor", "ins-suspended@example.com", UserStatus.SUSPENDED);

    instructorProfile(active, true, "Active");
    instructorProfile(suspended, true, "Suspended");
    instructorProfile(
        user("Private Instructor", "ins-private@example.com", UserStatus.ACTIVE), false, "Private");

    var publicActive = instructorProfileRepository.findPublicByUserStatus(UserStatus.ACTIVE);
    assertThat(publicActive).hasSize(1);
    assertThat(publicActive.getFirst().getUserId()).isEqualTo(active.getId());
  }

  @Test
  void publishedCourseCountQueryIgnoresDraftCourses() {
    User creator = user("Creator", "creator10@example.com", UserStatus.ACTIVE);
    User instructor = user("Instructor", "ins-count@example.com", UserStatus.ACTIVE);
    instructorProfile(instructor, true, "Count");

    Course published =
        course(
            "Published",
            uniqueSlug("count-p"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    Course draft =
        course(
            "Draft",
            uniqueSlug("count-d"),
            PublishStatus.DRAFT,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            null);
    attachInstructor(published, instructor);
    attachInstructor(draft, instructor);

    var rows =
        courseInstructorRepository.countByInstructorIdsAndCourseStatus(
            java.util.List.of(instructor.getId()), PublishStatus.PUBLISHED);
    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst()[0]).isEqualTo(instructor.getId());
    assertThat(rows.getFirst()[1]).isEqualTo(1L);
  }

  @Test
  void findPublicByUserIdAndStatusReturnsOnlyActivePublic() {
    User active = user("Public Active", "public-active@example.com", UserStatus.ACTIVE);
    User inactive = user("Public Inactive", "public-inactive@example.com", UserStatus.SUSPENDED);
    User privateActive = user("Private Active", "private-active@example.com", UserStatus.ACTIVE);
    instructorProfile(active, true, "PA");
    instructorProfile(inactive, true, "PI");
    instructorProfile(privateActive, false, "PR");

    assertThat(
            instructorProfileRepository.findPublicByUserIdAndStatus(
                active.getId(), UserStatus.ACTIVE))
        .isPresent();
    assertThat(
            instructorProfileRepository.findPublicByUserIdAndStatus(
                inactive.getId(), UserStatus.ACTIVE))
        .isNotPresent();
    assertThat(
            instructorProfileRepository.findPublicByUserIdAndStatus(
                privateActive.getId(), UserStatus.ACTIVE))
        .isNotPresent();
  }
}
