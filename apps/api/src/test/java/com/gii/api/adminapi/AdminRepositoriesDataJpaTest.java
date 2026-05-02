package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gii.common.entity.course.CourseSection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class AdminRepositoriesDataJpaTest extends AbstractAdminDataJpaTest {

  @Autowired private com.gii.common.repository.course.CourseRepository courseRepository;

  @Autowired
  private com.gii.common.repository.course.CourseSectionRepository courseSectionRepository;

  @Autowired private com.gii.common.repository.course.LessonRepository lessonRepository;
  @Autowired private com.gii.common.repository.course.MediaAssetRepository mediaAssetRepository;

  @Test
  void courseSlugShouldBeUnique() {
    var creator = user("Creator Repo", "creator-repo@example.com");
    courseRepository.saveAndFlush(course("Unique Slug A", "shared-slug", creator));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> courseRepository.saveAndFlush(course("Unique Slug B", "shared-slug", creator)));
  }

  @Test
  void sectionPositionShouldBeUniqueWithinCourse() {
    var creator = user("Creator Section Repo", "creator-sec-repo@example.com");
    var course = course("Section Repo", "section-repo", creator);
    courseSectionRepository.saveAndFlush(section(course, 1));

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            courseSectionRepository.saveAndFlush(
                CourseSection.builder()
                    .course(course)
                    .title("Section Duplicate")
                    .slug("section-dup")
                    .position(1)
                    .build()));
  }

  @Test
  void mediaAssetExistsByLessonIdShouldReflectPersistence() {
    var creator = user("Creator Media Repo", "creator-media-repo@example.com");
    var course = course("Media Repo", "media-repo", creator);
    var sec = section(course, 1);
    var lesson = lesson(course, sec, 1);
    assertThat(mediaAssetRepository.existsByLessonId(lesson.getId())).isFalse();
    mediaAssetRepository.saveAndFlush(mediaAsset(lesson, "exists-playback"));
    assertThat(mediaAssetRepository.existsByLessonId(lesson.getId())).isTrue();
  }
}
