package com.gii.api.studentapi;

import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionCourseId;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.enrollment.LessonProgressId;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.user.User;
import com.gii.common.entity.user.UserProfile;
import com.gii.common.enums.CertificateTargetType;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.certificate.CertificateRepository;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseReviewRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.quiz.QuizAttemptRepository;
import com.gii.common.repository.quiz.QuizRepository;
import com.gii.common.repository.user.UserProfileRepository;
import com.gii.common.repository.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

abstract class StudentApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected UserProfileRepository userProfileRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected CourseReviewRepository courseReviewRepository;
  @Autowired protected CollectionRepository collectionRepository;
  @Autowired protected CollectionCourseRepository collectionCourseRepository;
  @Autowired protected CollectionEnrollmentRepository collectionEnrollmentRepository;
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected EnrollmentRepository enrollmentRepository;
  @Autowired protected LessonProgressRepository lessonProgressRepository;
  @Autowired protected OrderRepository orderRepository;
  @Autowired protected OrderItemRepository orderItemRepository;
  @Autowired protected CertificateRepository certificateRepository;
  @Autowired protected QuizRepository quizRepository;
  @Autowired protected QuizAttemptRepository quizAttemptRepository;
  @Autowired protected LiveClassRepository liveClassRepository;
  @Autowired protected LiveClassRegistrantRepository liveClassRegistrantRepository;

  protected void cleanupStudentData() {
    courseReviewRepository.deleteAll();
    certificateRepository.deleteAll();
    collectionEnrollmentRepository.deleteAll();
    collectionCourseRepository.deleteAll();
    collectionRepository.deleteAll();
    liveClassRegistrantRepository.deleteAll();
    liveClassRepository.deleteAll();
    lessonProgressRepository.deleteAll();
    enrollmentRepository.deleteAll();
    orderItemRepository.deleteAll();
    orderRepository.deleteAll();
    quizAttemptRepository.deleteAll();
    quizRepository.deleteAll();
    lessonRepository.deleteAll();
    courseSectionRepository.deleteAll();
    courseRepository.deleteAll();
    userProfileRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected Collection collection(
      String title, String slug, User creator, com.gii.common.enums.PublishStatus status) {
    return collectionRepository.save(
        Collection.builder()
            .title(title)
            .slug(slug)
            .type(CollectionType.PACK)
            .priceBdt(BigDecimal.valueOf(2500))
            .status(status)
            .publishedAt(
                status == com.gii.common.enums.PublishStatus.PUBLISHED ? Instant.now() : null)
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

  protected UserProfile profile(User user, String avatarUrl) {
    return userProfileRepository.save(
        UserProfile.builder().user(user).avatarUrl(avatarUrl).locale("en-US").build());
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
            .estimatedDurationMinutes(300)
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
      Course course, CourseSection section, int position, PublishStatus status, boolean isFree) {
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
            .build());
  }

  protected Quiz quiz(
      Course course, CourseSection section, int position, PublishStatus status, String title) {
    return quizRepository.save(
        Quiz.builder()
            .course(course)
            .section(section)
            .position(position)
            .title(title)
            .status(status)
            .passingScorePct(60)
            .maxAttempts(3)
            .timeLimitSec(600)
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
            .completedAt(Instant.now().minusSeconds(3600))
            .lastPositionSec(120)
            .updatedAt(Instant.now().minusSeconds(3600))
            .build());
  }

  protected Order order(User user, OrderStatus status, BigDecimal amount) {
    return orderRepository.save(
        Order.builder()
            .user(user)
            .provider(OrderProvider.SSLCOMMERZ)
            .status(status)
            .amountBdt(amount)
            .currency("BDT")
            .paidAt(status == OrderStatus.PAID ? Instant.now().minusSeconds(7200) : null)
            .build());
  }

  protected OrderItem orderItem(Order order, Course course, BigDecimal price, BigDecimal discount) {
    return orderItemRepository.save(
        OrderItem.builder()
            .order(order)
            .itemType(OrderItemType.COURSE)
            .course(course)
            .titleSnapshot(course.getTitle())
            .priceBdt(price)
            .discountBdt(discount)
            .build());
  }

  protected Certificate certificate(User user, Course course, String code, boolean revoked) {
    return certificateRepository.save(
        Certificate.builder()
            .certificateCode(code)
            .user(user)
            .targetType(CertificateTargetType.COURSE)
            .course(course)
            .targetTitle(course.getTitle())
            .targetSlug(course.getSlug())
            .recipientName(user.getFullName())
            .issuedAt(Instant.now().minusSeconds(86400))
            .revokedAt(revoked ? Instant.now().minusSeconds(1000) : null)
            .pdfUrl("https://cdn.test/" + code + ".pdf")
            .build());
  }

  protected LiveClass liveClass(
      Course course,
      CourseSection section,
      Lesson ignoredLesson,
      User instructor,
      LiveClassStatus status,
      Instant startsAt,
      Instant endsAt,
      String joinUrl) {
    return liveClassRepository.save(
        LiveClass.builder()
            .course(course)
            .section(section)
            .instructor(instructor)
            .title("Live Session")
            .provider(LiveClassProvider.ZOOM)
            .status(status)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .participantJoinUrl(joinUrl)
            .providerMeetingId("m-" + UUID.randomUUID())
            .build());
  }

  protected LiveClassRegistrant registrant(
      User user, LiveClass liveClass, LiveClassRegistrantStatus status) {
    return liveClassRegistrantRepository.save(
        LiveClassRegistrant.builder()
            .user(user)
            .liveClass(liveClass)
            .status(status)
            .participantJoinUrl("https://zoom.test/join/" + liveClass.getId())
            .providerRegistrantId("r-" + UUID.randomUUID())
            .build());
  }
}
