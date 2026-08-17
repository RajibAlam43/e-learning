package com.gii.api.certificateapi;

import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionCourseId;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseInstructorId;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.enrollment.LessonProgressId;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CertificateTargetType;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
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

abstract class CertificateApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected CollectionRepository collectionRepository;
  @Autowired protected CollectionCourseRepository collectionCourseRepository;
  @Autowired protected CollectionEnrollmentRepository collectionEnrollmentRepository;
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected EnrollmentRepository enrollmentRepository;
  @Autowired protected LessonProgressRepository lessonProgressRepository;
  @Autowired protected CertificateRepository certificateRepository;
  @Autowired protected CourseInstructorRepository courseInstructorRepository;

  protected void cleanupCertificateData() {
    certificateRepository.deleteAll();
    collectionEnrollmentRepository.deleteAll();
    collectionCourseRepository.deleteAll();
    collectionRepository.deleteAll();
    lessonProgressRepository.deleteAll();
    courseInstructorRepository.deleteAll();
    enrollmentRepository.deleteAll();
    lessonRepository.deleteAll();
    courseSectionRepository.deleteAll();
    courseRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected Collection collection(String title, String slug, User creator, PublishStatus status) {
    return collectionRepository.save(
        Collection.builder()
            .title(title)
            .slug(slug)
            .type(CollectionType.PACK)
            .status(status)
            .priceBdt(BigDecimal.valueOf(2400))
            .publishedAt(status == PublishStatus.PUBLISHED ? Instant.now() : null)
            .createdBy(creator)
            .build());
  }

  protected CollectionCourse collectionCourse(
      Collection collection, Course course, int position, boolean isMandatory) {
    return collectionCourseRepository.save(
        CollectionCourse.builder()
            .id(
                CollectionCourseId.builder()
                    .collectionId(collection.getId())
                    .courseId(course.getId())
                    .build())
            .collection(collection)
            .course(course)
            .position(position)
            .isMandatory(isMandatory)
            .build());
  }

  protected CollectionEnrollment collectionEnrollment(
      User user, Collection collection, EnrollmentStatus status, Instant expiresAt) {
    return collectionEnrollmentRepository.save(
        CollectionEnrollment.builder()
            .user(user)
            .collection(collection)
            .status(status)
            .enrolledAt(Instant.now().minusSeconds(3600))
            .expiresAt(expiresAt)
            .build());
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
            .recordedHoursCount(2)
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
      Course course, CourseSection section, int position, PublishStatus status) {
    return lessonRepository.save(
        Lesson.builder()
            .course(course)
            .section(section)
            .title("Lesson " + position)
            .slug("lesson-" + position + "-" + UUID.randomUUID().toString().substring(0, 6))
            .position(position)
            .lessonType(LessonType.VIDEO)
            .status(status)
            .isFree(false)
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

  protected LessonProgress completedProgress(User user, Lesson lesson) {
    return lessonProgressRepository.save(
        LessonProgress.builder()
            .id(LessonProgressId.builder().userId(user.getId()).lessonId(lesson.getId()).build())
            .user(user)
            .lesson(lesson)
            .completedAt(Instant.now().minusSeconds(60))
            .lastPositionSec(100)
            .updatedAt(Instant.now().minusSeconds(60))
            .build());
  }

  protected CourseInstructor primaryInstructor(Course course, User instructor) {
    return courseInstructorRepository.save(
        CourseInstructor.builder()
            .id(
                CourseInstructorId.builder()
                    .courseId(course.getId())
                    .instructorUserId(instructor.getId())
                    .build())
            .course(course)
            .instructor(instructor)
            .role(InstructorRole.PRIMARY)
            .build());
  }

  protected Certificate certificate(
      User user, Course course, String code, boolean revoked, String pdfUrl, User issuedBy) {
    return certificateRepository.save(
        Certificate.builder()
            .certificateCode(code)
            .user(user)
            .targetType(CertificateTargetType.COURSE)
            .course(course)
            .issuedBy(issuedBy)
            .recipientName(user.getFullName())
            .targetTitle(course.getTitle())
            .targetSlug(course.getSlug())
            .pdfUrl(pdfUrl)
            .issuedAt(Instant.now().minusSeconds(3600))
            .revokedAt(revoked ? Instant.now().minusSeconds(1800) : null)
            .build());
  }
}
