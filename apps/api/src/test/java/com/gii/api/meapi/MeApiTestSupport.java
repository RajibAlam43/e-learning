package com.gii.api.meapi;

import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassAttendance;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.entity.user.User;
import com.gii.common.entity.user.UserProfile;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.live.LiveClassAttendanceRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import com.gii.common.repository.user.UserProfileRepository;
import com.gii.common.repository.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

abstract class MeApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected UserProfileRepository userProfileRepository;
  @Autowired protected InstructorProfileRepository instructorProfileRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected EnrollmentRepository enrollmentRepository;
  @Autowired protected CertificateRepository certificateRepository;
  @Autowired protected LiveClassRepository liveClassRepository;
  @Autowired protected LiveClassAttendanceRepository liveClassAttendanceRepository;

  protected void cleanupMeData() {
    liveClassAttendanceRepository.deleteAll();
    liveClassRepository.deleteAll();
    certificateRepository.deleteAll();
    enrollmentRepository.deleteAll();
    instructorProfileRepository.deleteAll();
    userProfileRepository.deleteAll();
    lessonRepository.deleteAll();
    courseSectionRepository.deleteAll();
    courseRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected Authentication userAuth(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }

  protected User user(String fullName, String email, String phone) {
    return userRepository.save(
        User.builder()
            .fullName(fullName)
            .email(email)
            .phone(phone)
            .phoneCountryCode("+880")
            .passwordHash("x")
            .status(UserStatus.ACTIVE)
            .build());
  }

  protected UserProfile profile(
      User user, String locale, String timezone, String avatar, String bio) {
    return userProfileRepository.save(
        UserProfile.builder()
            .user(user)
            .locale(locale)
            .timezone(timezone)
            .avatarUrl(avatar)
            .bio(bio)
            .extraJson(Map.of("k", "v"))
            .build());
  }

  protected InstructorProfile instructorProfile(User user, String displayName) {
    return instructorProfileRepository.save(
        InstructorProfile.builder()
            .user(user)
            .displayName(displayName)
            .headline("headline")
            .institution("inst")
            .expertiseArea("fiqh")
            .about("about")
            .photoUrl("https://cdn.test/p.jpg")
            .isPublic(true)
            .credentialsText("cred")
            .specialties(java.util.List.of("a", "b"))
            .yearsExperience(8)
            .build());
  }

  protected Course course(String title, String slug, User creator) {
    return courseRepository.save(
        Course.builder()
            .title(title)
            .slug(slug)
            .priceBdt(BigDecimal.valueOf(1000))
            .level(CourseLevel.BEGINNER)
            .language(CourseLanguage.EN)
            .studyMode(StudyMode.SCHEDULED)
            .status(PublishStatus.PUBLISHED)
            .publishedAt(Instant.now())
            .createdBy(creator)
            .liveSessionCount(0)
            .quizCount(0)
            .recordedHoursCount(0)
            .estimatedDurationMinutes(60)
            .build());
  }

  protected CourseSection section(Course course, int position) {
    return courseSectionRepository.save(
        CourseSection.builder()
            .course(course)
            .title("Section " + position)
            .slug("section-" + position + "-" + UUID.randomUUID().toString().substring(0, 6))
            .position(position)
            .status(PublishStatus.PUBLISHED)
            .build());
  }

  protected Lesson lesson(Course course, CourseSection section, int position) {
    return lessonRepository.save(
        Lesson.builder()
            .course(course)
            .section(section)
            .title("Lesson " + position)
            .slug("lesson-" + position + "-" + UUID.randomUUID().toString().substring(0, 6))
            .position(position)
            .lessonType(com.gii.common.enums.LessonType.VIDEO)
            .status(PublishStatus.PUBLISHED)
            .isFree(false)
            .build());
  }

  protected Enrollment enrollment(User user, Course course, boolean completed) {
    return enrollmentRepository.save(
        Enrollment.builder()
            .user(user)
            .course(course)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now().minusSeconds(86400))
            .completedAt(completed ? Instant.now().minusSeconds(3600) : null)
            .build());
  }

  protected Certificate certificate(
      User user, Course course, String code, boolean revoked, User issuedBy) {
    return certificateRepository.save(
        Certificate.builder()
            .certificateCode(code)
            .user(user)
            .course(course)
            .issuedBy(issuedBy)
            .recipientName(user.getFullName())
            .courseTitle(course.getTitle())
            .issuedAt(Instant.now().minusSeconds(7200))
            .revokedAt(revoked ? Instant.now().minusSeconds(100) : null)
            .build());
  }

  protected LiveClass liveClass(
      Course course, CourseSection section, Lesson lesson, User instructor) {
    return liveClassRepository.save(
        LiveClass.builder()
            .course(course)
            .section(section)
            .lesson(lesson)
            .instructor(instructor)
            .title("lc")
            .provider(LiveClassProvider.ZOOM)
            .status(LiveClassStatus.COMPLETED)
            .startsAt(Instant.now().minusSeconds(5000))
            .endsAt(Instant.now().minusSeconds(4000))
            .build());
  }

  protected LiveClassAttendance attendance(User user, LiveClass liveClass) {
    return liveClassAttendanceRepository.save(
        LiveClassAttendance.builder()
            .user(user)
            .liveClass(liveClass)
            .joinedAt(Instant.now().minusSeconds(3000))
            .leftAt(Instant.now().minusSeconds(2900))
            .durationSec(100)
            .build());
  }
}
