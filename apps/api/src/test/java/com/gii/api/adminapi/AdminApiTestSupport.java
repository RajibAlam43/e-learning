package com.gii.api.adminapi;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseInstructorId;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizChoice;
import com.gii.common.entity.quiz.QuizQuestion;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.entity.user.Role;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.MediaStatus;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.QuestionType;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.quiz.QuizAttemptAnswerRepository;
import com.gii.common.repository.quiz.QuizAttemptRepository;
import com.gii.common.repository.quiz.QuizChoiceRepository;
import com.gii.common.repository.quiz.QuizQuestionRepository;
import com.gii.common.repository.quiz.QuizRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import com.gii.common.repository.user.RoleRepository;
import com.gii.common.repository.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

abstract class AdminApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected MediaAssetRepository mediaAssetRepository;
  @Autowired protected EnrollmentRepository enrollmentRepository;
  @Autowired protected LiveClassRepository liveClassRepository;
  @Autowired protected LiveClassRegistrantRepository liveClassRegistrantRepository;
  @Autowired protected QuizRepository quizRepository;
  @Autowired protected QuizQuestionRepository quizQuestionRepository;
  @Autowired protected QuizChoiceRepository quizChoiceRepository;
  @Autowired protected QuizAttemptRepository quizAttemptRepository;
  @Autowired protected QuizAttemptAnswerRepository quizAttemptAnswerRepository;
  @Autowired protected CourseInstructorRepository courseInstructorRepository;
  @Autowired protected InstructorProfileRepository instructorProfileRepository;
  @Autowired protected RoleRepository roleRepository;
  @Autowired protected OrderItemRepository orderItemRepository;
  @Autowired protected OrderRepository orderRepository;

  protected void cleanupAdminData() {
    quizAttemptAnswerRepository.deleteAll();
    quizAttemptRepository.deleteAll();
    quizChoiceRepository.deleteAll();
    quizQuestionRepository.deleteAll();
    quizRepository.deleteAll();
    liveClassRegistrantRepository.deleteAll();
    liveClassRepository.deleteAll();
    enrollmentRepository.deleteAll();
    orderItemRepository.deleteAll();
    orderRepository.deleteAll();
    mediaAssetRepository.deleteAll();
    lessonRepository.deleteAll();
    courseSectionRepository.deleteAll();
    courseInstructorRepository.deleteAll();
    courseRepository.deleteAll();
    instructorProfileRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected Authentication adminAuth(UUID userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
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

  protected Course course(String title, String slug, User creator) {
    return courseRepository.save(
        Course.builder()
            .title(title)
            .slug(slug)
            .priceBdt(BigDecimal.valueOf(1500))
            .level(CourseLevel.BEGINNER)
            .language(CourseLanguage.EN)
            .studyMode(StudyMode.SCHEDULED)
            .status(PublishStatus.DRAFT)
            .createdBy(creator)
            .isFree(false)
            .liveSessionCount(0)
            .quizCount(0)
            .recordedHoursCount(0)
            .build());
  }

  protected Course course(String title, String slug, User creator, PublishStatus status) {
    Course c = course(title, slug, creator);
    c.setStatus(status);
    if (status == PublishStatus.PUBLISHED) {
      c.setPublishedAt(Instant.now());
    }
    return courseRepository.save(c);
  }

  protected CourseSection section(Course course, int position) {
    return courseSectionRepository.save(
        CourseSection.builder()
            .course(course)
            .title("Section " + position)
            .slug("section-" + position + "-" + UUID.randomUUID().toString().substring(0, 6))
            .position(position)
            .status(PublishStatus.DRAFT)
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
            .lessonType(LessonType.VIDEO)
            .status(PublishStatus.DRAFT)
            .isFree(false)
            .isMandatory(false)
            .build());
  }

  protected MediaAsset mediaAsset(Lesson lesson, String playbackId) {
    return mediaAssetRepository.save(
        MediaAsset.builder()
            .lesson(lesson)
            .provider(MediaProvider.MUX)
            .providerAssetId("asset-" + UUID.randomUUID())
            .playbackId(playbackId)
            .title("Media " + UUID.randomUUID().toString().substring(0, 6))
            .status(MediaStatus.READY)
            .build());
  }

  protected void ensureInstructorRolePresent() {
    if (roleRepository.findByName("ROLE_INSTRUCTOR").isEmpty()) {
      roleRepository.save(Role.builder().name("ROLE_INSTRUCTOR").build());
    }
  }

  protected InstructorProfile instructorProfile(User user) {
    return instructorProfileRepository.save(
        InstructorProfile.builder()
            .user(user)
            .displayName(user.getFullName())
            .isPublic(true)
            .build());
  }

  protected CourseInstructor assignment(Course course, User instructor) {
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

  protected LiveClass liveClass(Course course, CourseSection section, Lesson lesson) {
    return liveClassRepository.save(
        LiveClass.builder()
            .course(course)
            .section(section)
            .lesson(lesson)
            .title("Live Session")
            .provider(LiveClassProvider.OTHER)
            .participantJoinUrl("https://zoom.test/join")
            .status(LiveClassStatus.SCHEDULED)
            .startsAt(Instant.now().plusSeconds(1800))
            .endsAt(Instant.now().plusSeconds(3600))
            .build());
  }

  protected com.gii.common.entity.enrollment.Enrollment enrollment(
      User user, Course course, com.gii.common.enums.EnrollmentStatus status) {
    return enrollmentRepository.save(
        com.gii.common.entity.enrollment.Enrollment.builder()
            .user(user)
            .course(course)
            .status(status)
            .enrolledAt(Instant.now().minusSeconds(3600))
            .build());
  }

  protected LiveClassRegistrant registrant(
      LiveClass liveClass, User user, LiveClassRegistrantStatus status) {
    return liveClassRegistrantRepository.save(
        LiveClassRegistrant.builder().liveClass(liveClass).user(user).status(status).build());
  }

  protected Quiz quiz(Course course, String title) {
    return quizRepository.save(
        Quiz.builder()
            .course(course)
            .title(title)
            .status(PublishStatus.DRAFT)
            .passingScorePct(70)
            .maxAttempts(2)
            .timeLimitSec(900)
            .build());
  }

  protected QuizQuestion question(Quiz quiz, int position, String text) {
    return quizQuestionRepository.save(
        QuizQuestion.builder()
            .quiz(quiz)
            .position(position)
            .questionText(text)
            .questionType(QuestionType.MCQ)
            .points(1)
            .build());
  }

  protected QuizChoice choice(QuizQuestion question, String text, boolean correct) {
    return quizChoiceRepository.save(
        QuizChoice.builder().question(question).choiceText(text).isCorrect(correct).build());
  }

  protected Order order(User user, OrderStatus status) {
    return orderRepository.save(
        Order.builder()
            .user(user)
            .amountBdt(BigDecimal.valueOf(1200))
            .provider(OrderProvider.SSLCOMMERZ)
            .providerTxnId("TXN-" + UUID.randomUUID())
            .status(status)
            .build());
  }

  protected OrderItem orderItem(Order order, Course course, BigDecimal price, BigDecimal discount) {
    return orderItemRepository.save(
        OrderItem.builder()
            .order(order)
            .course(course)
            .priceBdt(price)
            .discountBdt(discount)
            .build());
  }
}
