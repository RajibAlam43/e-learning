package com.gii.api.lessonapi;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.LessonResource;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.enrollment.LessonProgressId;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LessonResourceType;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.MediaAssetType;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.MediaStatus;
import com.gii.common.enums.PlaybackMode;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.LessonResourceRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import com.gii.common.repository.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

abstract class LessonApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected EnrollmentRepository enrollmentRepository;
  @Autowired protected LessonProgressRepository lessonProgressRepository;
  @Autowired protected MediaAssetRepository mediaAssetRepository;
  @Autowired protected LessonResourceRepository lessonResourceRepository;

  protected void cleanupLessonData() {
    lessonProgressRepository.deleteAll();
    lessonResourceRepository.deleteAll();
    mediaAssetRepository.deleteAll();
    enrollmentRepository.deleteAll();
    lessonRepository.deleteAll();
    courseSectionRepository.deleteAll();
    courseRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected Authentication studentAuth(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }

  protected User user(String fullName, String email) {
    return userRepository.save(
        User.builder()
            .fullName(fullName)
            .email(email)
            .passwordHash("x")
            .status(UserStatus.ACTIVE)
            .build());
  }

  protected Course course(String title, String slug, User creator, PublishStatus status) {
    return courseRepository.save(
        Course.builder()
            .title(title)
            .slug(slug)
            .priceBdt(BigDecimal.valueOf(1200))
            .level(CourseLevel.BEGINNER)
            .language(CourseLanguage.EN)
            .studyMode(StudyMode.SCHEDULED)
            .status(status)
            .publishedAt(Instant.now())
            .createdBy(creator)
            .liveSessionCount(0)
            .quizCount(0)
            .recordedHoursCount(0)
            .estimatedDurationMinutes(120)
            .build());
  }

  protected CourseSection section(Course course, int position, PublishStatus status) {
    return courseSectionRepository.save(
        CourseSection.builder()
            .course(course)
            .title("Section " + position)
            .slug("section-" + position + "-" + UUID.randomUUID().toString().substring(0, 6))
            .position(position)
            .status(status)
            .build());
  }

  protected Lesson lesson(
      Course course,
      CourseSection section,
      int position,
      PublishStatus status,
      boolean isFree,
      ReleaseType releaseType,
      Instant releaseAt,
      Integer unlockAfterDays) {
    return lessonRepository.save(
        Lesson.builder()
            .course(course)
            .section(section)
            .title("Lesson " + position)
            .slug("lesson-" + position + "-" + UUID.randomUUID().toString().substring(0, 6))
            .position(position)
            .lessonType(LessonType.VIDEO)
            .status(status)
            .isFree(isFree)
            .releaseType(releaseType)
            .releaseAt(releaseAt)
            .unlockAfterDays(unlockAfterDays)
            .durationSeconds(300)
            .build());
  }

  protected Enrollment enrollment(
      User user, Course course, EnrollmentStatus status, Instant expiresAt) {
    return enrollmentRepository.save(
        Enrollment.builder()
            .user(user)
            .course(course)
            .status(status)
            .enrolledAt(Instant.now().minusSeconds(86400))
            .expiresAt(expiresAt)
            .build());
  }

  protected MediaAsset mediaAsset(Lesson lesson, MediaProvider provider, MediaStatus status) {
    return mediaAssetRepository.save(
        MediaAsset.builder()
            .lesson(lesson)
            .provider(provider)
            .assetType(MediaAssetType.VIDEO)
            .providerAssetId("asset-" + UUID.randomUUID())
            .providerLibraryId("lib-123")
            .playbackId("playback-123")
            .fileUrl("https://cdn.test/file.mp4")
            .title("Media " + lesson.getTitle())
            .maxResolution("1080p")
            .durationSec(300)
            .status(status)
            .preferredPlaybackMode(PlaybackMode.HLS)
            .build());
  }

  protected LessonResource resource(Lesson lesson, int position, String title) {
    return lessonResourceRepository.save(
        LessonResource.builder()
            .lesson(lesson)
            .resourceType(LessonResourceType.PDF)
            .title(title)
            .fileUrl("https://cdn.test/resources/" + title.replace(" ", "-") + ".pdf")
            .mimeType("application/pdf")
            .position(position)
            .build());
  }

  protected LessonProgress progress(
      User user, Lesson lesson, boolean completed, int lastPositionSec) {
    return lessonProgressRepository.save(
        LessonProgress.builder()
            .id(LessonProgressId.builder().userId(user.getId()).lessonId(lesson.getId()).build())
            .user(user)
            .lesson(lesson)
            .completedAt(completed ? Instant.now().minusSeconds(10) : null)
            .lastPositionSec(lastPositionSec)
            .updatedAt(Instant.now().minusSeconds(10))
            .build());
  }
}
