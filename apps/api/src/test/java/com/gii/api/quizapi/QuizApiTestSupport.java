package com.gii.api.quizapi;

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
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.QuestionType;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
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
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected EnrollmentRepository enrollmentRepository;
  @Autowired protected QuizRepository quizRepository;
  @Autowired protected QuizQuestionRepository quizQuestionRepository;
  @Autowired protected QuizChoiceRepository quizChoiceRepository;
  @Autowired protected QuizAttemptRepository quizAttemptRepository;
  @Autowired protected QuizAttemptAnswerRepository quizAttemptAnswerRepository;

  protected void cleanupQuizData() {
    quizAttemptAnswerRepository.deleteAll();
    quizAttemptRepository.deleteAll();
    quizChoiceRepository.deleteAll();
    quizQuestionRepository.deleteAll();
    quizRepository.deleteAll();
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
            .quizCount(1)
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
      Course course, CourseSection section, int position, PublishStatus status) {
    return lessonRepository.save(
        Lesson.builder()
            .course(course)
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
            .course(course)
            .lesson(lesson)
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
    return quizAttemptRepository.save(
        QuizAttempt.builder()
            .quiz(quiz)
            .user(user)
            .attemptNo(attemptNo)
            .scorePct(scorePct)
            .passed(passed)
            .startedAt(startedAt)
            .submittedAt(submittedAt)
            .build());
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
