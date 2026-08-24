package com.gii.api.quizapi;

import com.gii.api.testsupport.CourseTestData;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.entity.quiz.QuizAttemptAnswer;
import com.gii.common.entity.quiz.QuizAttemptAnswerId;
import com.gii.common.entity.quiz.QuizChoice;
import com.gii.common.entity.quiz.QuizQuestion;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.QuestionType;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.CourseTemplateRepository;
import com.gii.common.repository.course.CourseTemplateVersionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.enrollment.StudentLearningStreakRepository;
import com.gii.common.repository.quiz.QuizAttemptAnswerRepository;
import com.gii.common.repository.quiz.QuizAttemptRepository;
import com.gii.common.repository.quiz.QuizChoiceRepository;
import com.gii.common.repository.quiz.QuizQuestionRepository;
import com.gii.common.repository.quiz.QuizRepository;
import com.gii.common.repository.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

abstract class QuizApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected CourseTemplateVersionRepository courseTemplateVersionRepository;
  @Autowired protected CourseTemplateRepository courseTemplateRepository;
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected EnrollmentRepository enrollmentRepository;
  @Autowired protected QuizRepository quizRepository;
  @Autowired protected QuizQuestionRepository quizQuestionRepository;
  @Autowired protected QuizChoiceRepository quizChoiceRepository;
  @Autowired protected QuizAttemptRepository quizAttemptRepository;
  @Autowired protected QuizAttemptAnswerRepository quizAttemptAnswerRepository;
  @Autowired protected StudentLearningStreakRepository studentLearningStreakRepository;

  protected void cleanupQuizData() {
    studentLearningStreakRepository.deleteAll();
    quizAttemptAnswerRepository.deleteAll();
    quizAttemptRepository.deleteAll();
    quizChoiceRepository.deleteAll();
    quizQuestionRepository.deleteAll();
    quizRepository.deleteAll();
    enrollmentRepository.deleteAll();
    lessonRepository.deleteAll();
    courseSectionRepository.deleteAll();
    courseRepository.deleteAll();
    courseTemplateVersionRepository.deleteAll();
    courseTemplateRepository.deleteAll();
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
    Course course = CourseTestData.course(title, slug, creator);
    course.setPriceBdt(BigDecimal.valueOf(1200));
    course.setStatus(status);
    course.setPublishedAt(Instant.now());
    course.setQuizCount(1);
    course.setEstimatedDurationMinutes(120);
    course.getTemplateVersion().setStatus(status);
    return courseRepository.save(course);
  }

  protected Course repeatedCourse(Course source, String slug, User creator) {
    return courseRepository.save(
        Course.builder()
            .templateVersion(source.getTemplateVersion())
            .name(source.getName())
            .slug(slug)
            .priceBdt(source.getPriceBdt())
            .studyMode(source.getStudyMode())
            .status(PublishStatus.PUBLISHED)
            .publishedAt(Instant.now())
            .isFree(source.getIsFree())
            .createdBy(creator)
            .build());
  }

  protected CourseSection section(Course course, int position, PublishStatus status) {
    return courseSectionRepository.save(
        CourseSection.builder()
            .templateVersion(course.getTemplateVersion())
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
            .section(section)
            .title("Lesson " + position)
            .slug("lesson-" + position + "-" + UUID.randomUUID().toString().substring(0, 6))
            .position(position)
            .lessonType(LessonType.QUIZ)
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

  protected Quiz quiz(
      Course course,
      Lesson lesson,
      String title,
      PublishStatus status,
      int passingScorePct,
      int maxAttempts,
      Integer timeLimitSec) {
    return quizRepository.save(
        Quiz.builder()
            .section(lesson.getSection())
            .position(lesson.getPosition())
            .title(title)
            .status(status)
            .passingScorePct(passingScorePct)
            .maxAttempts(maxAttempts)
            .timeLimitSec(timeLimitSec)
            .build());
  }

  protected QuizQuestion question(Quiz quiz, int position, String text, int points) {
    return quizQuestionRepository.save(
        QuizQuestion.builder()
            .quiz(quiz)
            .position(position)
            .questionText(text)
            .questionType(QuestionType.MCQ)
            .points(points)
            .explanationText("Explanation for " + text)
            .build());
  }

  protected QuizChoice choice(QuizQuestion question, String text, boolean correct) {
    return quizChoiceRepository.save(
        QuizChoice.builder().question(question).choiceText(text).isCorrect(correct).build());
  }

  protected QuizAttempt attempt(
      Quiz quiz,
      User user,
      int attemptNo,
      Integer scorePct,
      Boolean passed,
      Instant startedAt,
      Instant submittedAt) {
    Enrollment quizEnrollment =
        enrollmentRepository
            .findByUserIdAndCourseId(user.getId(), courseForQuiz(quiz).getId())
            .orElseGet(
                () ->
                    enrollment(
                        user,
                        courseForQuiz(quiz),
                        EnrollmentStatus.ACTIVE,
                        Instant.now().plusSeconds(86400)));
    return attempt(quiz, user, quizEnrollment, attemptNo, scorePct, passed, startedAt, submittedAt);
  }

  protected QuizAttempt attempt(
      Quiz quiz,
      User user,
      Enrollment enrollment,
      int attemptNo,
      Integer scorePct,
      Boolean passed,
      Instant startedAt,
      Instant submittedAt) {
    return quizAttemptRepository.save(
        QuizAttempt.builder()
            .quiz(quiz)
            .user(user)
            .enrollment(enrollment)
            .attemptNo(attemptNo)
            .scorePct(scorePct)
            .passed(passed)
            .startedAt(startedAt)
            .submittedAt(submittedAt)
            .build());
  }

  private Course courseForQuiz(Quiz quiz) {
    return courseRepository.findAll().stream()
        .filter(
            course ->
                course
                    .getTemplateVersion()
                    .getId()
                    .equals(quiz.getSection().getTemplateVersion().getId()))
        .findFirst()
        .orElseThrow();
  }

  protected QuizAttemptAnswer attemptAnswer(
      QuizAttempt attempt, QuizQuestion question, QuizChoice choice) {
    return quizAttemptAnswerRepository.save(
        QuizAttemptAnswer.builder()
            .attempt(attempt)
            .question(question)
            .choice(choice)
            .id(
                QuizAttemptAnswerId.builder()
                    .attemptId(attempt.getId())
                    .questionId(question.getId())
                    .build())
            .build());
  }
}
