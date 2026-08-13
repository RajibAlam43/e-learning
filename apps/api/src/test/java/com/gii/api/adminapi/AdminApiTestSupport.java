package com.gii.api.adminapi;

import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionCourseId;
import com.gii.common.entity.course.Category;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseInstructorId;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.course.SectionItem;
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
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassRegistrantStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.MediaStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.QuestionType;
import com.gii.common.enums.SectionItemType;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CategoryRepository;
import com.gii.common.repository.course.CourseCategoryRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseReviewRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.LessonResourceRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import com.gii.common.repository.course.SectionItemRepository;
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
import com.gii.common.repository.setting.AppSettingRepository;
import com.gii.common.repository.support.SupportTicketRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import com.gii.common.repository.user.RoleRepository;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.repository.user.UserRoleRepository;
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
  @Autowired protected CollectionRepository collectionRepository;
  @Autowired protected CollectionCourseRepository collectionCourseRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected CourseReviewRepository courseReviewRepository;
  @Autowired protected CategoryRepository categoryRepository;
  @Autowired protected CourseCategoryRepository courseCategoryRepository;
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected LessonResourceRepository lessonResourceRepository;
  @Autowired protected MediaAssetRepository mediaAssetRepository;
  @Autowired protected SectionItemRepository sectionItemRepository;
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
  @Autowired protected UserRoleRepository userRoleRepository;
  @Autowired protected SupportTicketRepository supportTicketRepository;
  @Autowired protected OrderItemRepository orderItemRepository;
  @Autowired protected OrderRepository orderRepository;
  @Autowired protected AppSettingRepository appSettingRepository;

  protected void cleanupAdminData() {
    appSettingRepository.deleteAll();
    courseReviewRepository.deleteAll();
    supportTicketRepository.deleteAll();
    collectionCourseRepository.deleteAll();
    collectionRepository.deleteAll();
    quizAttemptAnswerRepository.deleteAll();
    quizAttemptRepository.deleteAll();
    quizChoiceRepository.deleteAll();
    quizQuestionRepository.deleteAll();
    quizRepository.deleteAll();
    sectionItemRepository.deleteAll();
    liveClassRegistrantRepository.deleteAll();
    liveClassRepository.deleteAll();
    enrollmentRepository.deleteAll();
    orderItemRepository.deleteAll();
    orderRepository.deleteAll();
    mediaAssetRepository.deleteAll();
    lessonResourceRepository.deleteAll();
    lessonRepository.deleteAll();
    courseSectionRepository.deleteAll();
    courseInstructorRepository.deleteAll();
    courseCategoryRepository.deleteAll();
    courseRepository.deleteAll();
    categoryRepository.deleteAll();
    instructorProfileRepository.deleteAll();
    userRoleRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected Collection collection(String title, String slug, User creator, PublishStatus status) {
    return collectionRepository.save(
        Collection.builder()
            .title(title)
            .slug(slug)
            .type(CollectionType.PACK)
            .status(status)
            .priceBdt(BigDecimal.valueOf(3000))
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

  protected Category category(String name, String nameEn, String slug) {
    return categoryRepository.save(Category.builder().name(name).nameEn(nameEn).slug(slug).build());
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
    Lesson lesson =
        lessonRepository.save(
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
    sectionItemRepository.save(
        SectionItem.builder()
            .section(section)
            .itemType(SectionItemType.LESSON)
            .itemId(lesson.getId())
            .position(position)
            .build());
    return lesson;
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
    if (roleRepository.findByName("INSTRUCTOR").isEmpty()) {
      roleRepository.save(Role.builder().name("INSTRUCTOR").build());
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
            .title("Live Session")
            .provider(LiveClassProvider.ZOOM)
            .providerMeetingId("m-" + UUID.randomUUID())
            .hostStartUrl("https://zoom.test/start")
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
    CourseSection section =
        courseSectionRepository.findByCourseIdOrderByPositionAsc(course.getId()).stream()
            .findFirst()
            .orElseGet(() -> section(course, 1));
    int position =
        sectionItemRepository.findBySectionIdOrderByPositionAsc(section.getId()).stream()
                .mapToInt(item -> item.getPosition())
                .max()
                .orElse(0)
            + 1;
    Quiz quiz =
        quizRepository.save(
            Quiz.builder()
                .course(course)
                .section(section)
                .position(position)
                .title(title)
                .status(PublishStatus.DRAFT)
                .passingScorePct(70)
                .maxAttempts(2)
                .timeLimitSec(900)
                .build());
    sectionItemRepository.save(
        SectionItem.builder()
            .section(section)
            .itemType(SectionItemType.QUIZ)
            .itemId(quiz.getId())
            .position(position)
            .build());
    return quiz;
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
            .itemType(OrderItemType.COURSE)
            .course(course)
            .titleSnapshot(course.getTitle())
            .priceBdt(price)
            .discountBdt(discount)
            .build());
  }
}
