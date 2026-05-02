package com.gii.api.instructorapi;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseInstructorId;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassAttendance;
import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.live.LiveClassAttendanceRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import com.gii.common.repository.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

abstract class InstructorApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected InstructorProfileRepository instructorProfileRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected CourseInstructorRepository courseInstructorRepository;
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected EnrollmentRepository enrollmentRepository;
  @Autowired protected LiveClassRepository liveClassRepository;
  @Autowired protected LiveClassRegistrantRepository liveClassRegistrantRepository;
  @Autowired protected LiveClassAttendanceRepository liveClassAttendanceRepository;

  protected void cleanupInstructorData() {
    liveClassAttendanceRepository.deleteAll();
    liveClassRegistrantRepository.deleteAll();
    liveClassRepository.deleteAll();
    enrollmentRepository.deleteAll();
    courseInstructorRepository.deleteAll();
    lessonRepository.deleteAll();
    courseSectionRepository.deleteAll();
    courseRepository.deleteAll();
    instructorProfileRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected Authentication instructorAuth(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, java.util.List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")));
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

  protected InstructorProfile instructorProfile(User user, String displayName) {
    return instructorProfileRepository.save(
        InstructorProfile.builder()
            .userId(user.getId())
            .user(user)
            .displayName(displayName)
            .headline("Senior Instructor")
            .photoUrl("https://cdn.test/" + user.getId() + ".jpg")
            .isPublic(true)
            .build());
  }

  protected Course course(String title, String slug, User creator, PublishStatus status) {
    return courseRepository.save(
        Course.builder()
            .title(title)
            .slug(slug)
            .priceBdt(BigDecimal.valueOf(1000))
            .level(CourseLevel.BEGINNER)
            .language(CourseLanguage.EN)
            .studyMode(StudyMode.SCHEDULED)
            .status(status)
            .publishedAt(Instant.now())
            .createdBy(creator)
            .liveSessionCount(0)
            .quizCount(1)
            .recordedHoursCount(2)
            .estimatedDurationMinutes(180)
            .build());
  }

  protected CourseInstructor assignment(Course course, User instructor, InstructorRole role) {
    return courseInstructorRepository.save(
        CourseInstructor.builder()
            .id(
                CourseInstructorId.builder()
                    .courseId(course.getId())
                    .instructorUserId(instructor.getId())
                    .build())
            .course(course)
            .instructor(instructor)
            .role(role)
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

  protected LiveClass liveClass(
      Course course,
      CourseSection section,
      Lesson lesson,
      User instructor,
      LiveClassStatus status,
      Instant startsAt,
      Instant endsAt) {
    return liveClassRepository.save(
        LiveClass.builder()
            .course(course)
            .section(section)
            .lesson(lesson)
            .instructor(instructor)
            .title("Live " + lesson.getTitle())
            .description("desc")
            .provider(LiveClassProvider.ZOOM)
            .providerMeetingId("m-" + UUID.randomUUID())
            .hostStartUrl("https://zoom.test/start/" + UUID.randomUUID())
            .participantJoinUrl("https://zoom.test/join/" + UUID.randomUUID())
            .zoomMeetingId("z-" + UUID.randomUUID())
            .zoomStartUrl("https://zoom.test/start-legacy/" + UUID.randomUUID())
            .zoomJoinUrl("https://zoom.test/join-legacy/" + UUID.randomUUID())
            .startsAt(startsAt)
            .endsAt(endsAt)
            .status(status)
            .createdBy(instructor)
            .build());
  }

  protected LiveClassRegistrant registrant(
      User student, LiveClass liveClass, LiveClassRegistrantStatus status) {
    return liveClassRegistrantRepository.save(
        LiveClassRegistrant.builder()
            .user(student)
            .liveClass(liveClass)
            .status(status)
            .zoomRegistrantId("r-" + UUID.randomUUID())
            .zoomJoinUrl("https://zoom.test/reg/" + UUID.randomUUID())
            .build());
  }

  protected LiveClassAttendance attendance(User student, LiveClass liveClass) {
    return liveClassAttendanceRepository.save(
        LiveClassAttendance.builder()
            .user(student)
            .liveClass(liveClass)
            .joinedAt(Instant.now().minusSeconds(120))
            .leftAt(Instant.now().minusSeconds(30))
            .durationSec(90)
            .participantName(student.getFullName())
            .participantEmail(student.getEmail())
            .build());
  }
}
