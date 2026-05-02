package com.gii.api.publicapi;

import com.gii.common.entity.course.Category;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseCategory;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.support.SupportTicket;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CategoryRepository;
import com.gii.common.repository.course.CourseCategoryRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import com.gii.common.repository.support.SupportTicketRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import com.gii.common.repository.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class PublicApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected MediaAssetRepository mediaAssetRepository;
  @Autowired protected CategoryRepository categoryRepository;
  @Autowired protected CourseCategoryRepository courseCategoryRepository;
  @Autowired protected CourseInstructorRepository courseInstructorRepository;
  @Autowired protected InstructorProfileRepository instructorProfileRepository;
  @Autowired protected SupportTicketRepository supportTicketRepository;

  @AfterEach
  void cleanDb() {
    mediaAssetRepository.deleteAll();
    lessonRepository.deleteAll();
    courseSectionRepository.deleteAll();
    courseCategoryRepository.deleteAll();
    courseInstructorRepository.deleteAll();
    courseRepository.deleteAll();
    categoryRepository.deleteAll();
    instructorProfileRepository.deleteAll();
    supportTicketRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected User user(String name, String email, UserStatus status) {
    return userRepository.save(
        User.builder().fullName(name).email(email).passwordHash("x").status(status).build());
  }

  protected Course course(
      String title,
      String slug,
      PublishStatus status,
      User creator,
      CourseLevel level,
      CourseLanguage language,
      Instant publishedAt) {
    return courseRepository.save(
        Course.builder()
            .title(title)
            .slug(slug)
            .priceBdt(BigDecimal.valueOf(1000))
            .level(level)
            .language(language)
            .studyMode(StudyMode.SCHEDULED)
            .status(status)
            .publishedAt(publishedAt)
            .createdBy(creator)
            .liveSessionCount(0)
            .quizCount(0)
            .recordedHoursCount(0)
            .isFree(false)
            .build());
  }

  protected Category category(String name, String slug) {
    return categoryRepository.save(Category.builder().name(name).slug(slug).build());
  }

  protected void attachCategory(Course course, Category category) {
    courseCategoryRepository.save(
        CourseCategory.builder().course(course).category(category).build());
  }

  protected void attachInstructor(Course course, User instructor) {
    courseInstructorRepository.save(
        CourseInstructor.builder()
            .course(course)
            .instructor(instructor)
            .role(InstructorRole.PRIMARY)
            .build());
  }

  protected InstructorProfile instructorProfile(User user, boolean isPublic, String displayName) {
    return instructorProfileRepository.save(
        InstructorProfile.builder()
            .user(user)
            .displayName(displayName)
            .isPublic(isPublic)
            .headline("headline")
            .credentialsText("credentials")
            .specialties(List.of("A"))
            .yearsExperience(5)
            .build());
  }

  protected CourseSection section(Course course, String slug, int position, PublishStatus status) {
    return courseSectionRepository.save(
        CourseSection.builder()
            .course(course)
            .title("Section " + position)
            .slug(slug)
            .position(position)
            .status(status)
            .build());
  }

  protected Lesson lesson(
      Course course,
      CourseSection section,
      String slug,
      int position,
      PublishStatus status,
      boolean isFree) {
    return lessonRepository.save(
        Lesson.builder()
            .course(course)
            .section(section)
            .title("Lesson " + position)
            .slug(slug)
            .position(position)
            .status(status)
            .isFree(isFree)
            .lessonType(LessonType.VIDEO)
            .build());
  }

  protected MediaAsset mediaAsset(Lesson lesson, String providerAssetId) {
    return mediaAssetRepository.save(
        MediaAsset.builder()
            .lesson(lesson)
            .provider(MediaProvider.YOUTUBE)
            .providerAssetId(providerAssetId)
            .title("Video")
            .build());
  }

  protected long supportTicketCount() {
    return supportTicketRepository.count();
  }

  protected SupportTicket latestSupportTicket() {
    return supportTicketRepository.findAll().stream().findFirst().orElseThrow();
  }

  protected String uniqueSlug(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }
}
