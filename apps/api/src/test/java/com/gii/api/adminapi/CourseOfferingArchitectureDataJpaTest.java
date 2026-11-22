package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.enrollment.LessonProgressId;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CourseOfferingArchitectureDataJpaTest extends AbstractAdminDataJpaTest {

  @Autowired private LessonProgressRepository lessonProgressRepository;

  @Test
  void offeringsCanShareCurriculumButKeepDeliveryAndProgressIsolated() {
    var creator = user("Creator", "offering-architecture-creator@example.com");
    var student = user("Student", "offering-architecture-student@example.com");
    var firstCourse = course("Reusable curriculum", "reusable-2026-spring", creator);
    var section = section(firstCourse, 1);
    var lesson = lesson(firstCourse, section, 1);
    var sourceLiveClass = liveClass(firstCourse, section, lesson);

    var secondCourse =
        courseRepository.saveAndFlush(
            Course.builder()
                .template(firstCourse.getTemplate())
                .slug("reusable-2026-fall")
                .name(firstCourse.getTitle())
                .priceBdt(BigDecimal.valueOf(1500))
                .studyMode(firstCourse.getStudyMode())
                .status(PublishStatus.DRAFT)
                .isFree(false)
                .createdBy(creator)
                .build());

    assertThat(secondCourse.getId()).isNotEqualTo(firstCourse.getId());
    assertThat(secondCourse.getTemplate().getId()).isEqualTo(firstCourse.getTemplate().getId());
    assertThat(courseSectionRepository.findByCourseIdOrderByPositionAsc(secondCourse.getId()))
        .extracting("id")
        .containsExactly(section.getId());
    assertThat(liveClassRepository.findByCourseIdOrderByStartsAtAsc(firstCourse.getId()))
        .extracting("id")
        .containsExactly(sourceLiveClass.getId());
    assertThat(liveClassRepository.findByCourseIdOrderByStartsAtAsc(secondCourse.getId()))
        .isEmpty();

    var firstEnrollment = enrollment(student, firstCourse, EnrollmentStatus.ACTIVE);
    var secondEnrollment = enrollment(student, secondCourse, EnrollmentStatus.ACTIVE);
    lessonProgressRepository.saveAndFlush(
        LessonProgress.builder()
            .id(
                LessonProgressId.builder()
                    .enrollmentId(firstEnrollment.getId())
                    .lessonId(lesson.getId())
                    .build())
            .enrollment(firstEnrollment)
            .lesson(lesson)
            .completedAt(Instant.now())
            .build());

    assertThat(
            lessonProgressRepository.findById(
                LessonProgressId.builder()
                    .enrollmentId(secondEnrollment.getId())
                    .lessonId(lesson.getId())
                    .build()))
        .isEmpty();
  }
}
