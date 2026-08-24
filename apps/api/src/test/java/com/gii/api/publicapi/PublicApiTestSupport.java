package com.gii.api.publicapi;

import com.gii.api.testsupport.CourseTestData;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionCourseId;
import com.gii.common.entity.course.Category;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseCategory;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseReview;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.course.SectionItem;
import com.gii.common.entity.support.SupportTicket;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.entity.user.User;
import com.gii.common.entity.user.UserRole;
import com.gii.common.entity.user.UserRoleId;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReviewStatus;
import com.gii.common.enums.SectionItemType;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CategoryRepository;
import com.gii.common.repository.course.CourseCategoryRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseReviewRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.CourseTemplateRepository;
import com.gii.common.repository.course.CourseTemplateVersionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import com.gii.common.repository.course.SectionItemRepository;
import com.gii.common.repository.support.SupportTicketRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import com.gii.common.repository.user.RoleRepository;
import com.gii.common.repository.user.UserRepository;
import com.gii.common.repository.user.UserRoleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class PublicApiTestSupport {

  @Autowired protected UserRepository userRepository;
  @Autowired protected RoleRepository roleRepository;
  @Autowired protected UserRoleRepository userRoleRepository;
  @Autowired protected CourseRepository courseRepository;
  @Autowired protected CourseTemplateVersionRepository courseTemplateVersionRepository;
  @Autowired protected CourseTemplateRepository courseTemplateRepository;
  @Autowired protected CourseReviewRepository courseReviewRepository;
  @Autowired protected CourseSectionRepository courseSectionRepository;
  @Autowired protected LessonRepository lessonRepository;
  @Autowired protected MediaAssetRepository mediaAssetRepository;
  @Autowired protected SectionItemRepository sectionItemRepository;
  @Autowired protected CategoryRepository categoryRepository;
  @Autowired protected CollectionRepository collectionRepository;
  @Autowired protected CollectionCourseRepository collectionCourseRepository;
  @Autowired protected CourseCategoryRepository courseCategoryRepository;
  @Autowired protected CourseInstructorRepository courseInstructorRepository;
  @Autowired protected InstructorProfileRepository instructorProfileRepository;
  @Autowired protected SupportTicketRepository supportTicketRepository;

  @AfterEach
  void cleanDb() {
    courseReviewRepository.deleteAll();
    collectionCourseRepository.deleteAll();
    collectionRepository.deleteAll();
    mediaAssetRepository.deleteAll();
    sectionItemRepository.deleteAll();
    lessonRepository.deleteAll();
    courseSectionRepository.deleteAll();
    courseCategoryRepository.deleteAll();
    courseInstructorRepository.deleteAll();
    courseRepository.deleteAll();
    courseTemplateVersionRepository.deleteAll();
    courseTemplateRepository.deleteAll();
    categoryRepository.deleteAll();
    instructorProfileRepository.deleteAll();
    supportTicketRepository.deleteAll();
    userRoleRepository.deleteAll();
    userRepository.deleteAll();
  }

  protected Collection collection(
      String title,
      String slug,
      CollectionType type,
      com.gii.common.enums.PublishStatus status,
      User creator,
      Instant publishedAt) {
    return collectionRepository.save(
        Collection.builder()
            .title(title)
            .slug(slug)
            .type(type)
            .status(status)
            .priceBdt(BigDecimal.valueOf(3000))
            .publishedAt(publishedAt)
            .createdBy(creator)
            .build());
  }

  protected void attachCourseToCollection(
      Collection collection, Course course, int position, boolean isMandatory) {
    collectionCourseRepository.save(
        CollectionCourse.builder()
            .id(
                CollectionCourseId.builder()
                    .collectionId(collection.getId())
                    .courseOfferingId(course.getId())
                    .build())
            .collection(collection)
            .course(course)
            .position(position)
            .isMandatory(isMandatory)
            .build());
  }

  protected User user(String name, String email, UserStatus status) {
    return userRepository.save(
        User.builder().fullName(name).email(email).passwordHash("x").status(status).build());
  }

  protected void assignRole(User user, String roleName) {
    var role = roleRepository.findByName(roleName).orElseThrow();
    userRoleRepository.save(
        UserRole.builder()
            .id(UserRoleId.builder().userId(user.getId()).roleId(role.getId()).build())
            .user(user)
            .role(role)
            .build());
  }

  protected Course course(
      String title,
      String slug,
      PublishStatus status,
      User creator,
      CourseLevel level,
      CourseLanguage language,
      Instant publishedAt) {
    Course course = CourseTestData.course(title, slug, creator);
    course.setPriceBdt(BigDecimal.valueOf(1000));
    course.setLevel(level);
    course.setLanguage(language);
    course.setStatus(status);
    course.setPublishedAt(publishedAt);
    course.getTemplateVersion().setStatus(status);
    return courseRepository.save(course);
  }

  protected Category category(String name, String slug) {
    return categoryRepository.save(Category.builder().name(name).nameEn(name).slug(slug).build());
  }

  protected void attachCategory(Course course, Category category) {
    courseCategoryRepository.save(
        CourseCategory.builder()
            .templateVersion(course.getTemplateVersion())
            .category(category)
            .build());
  }

  protected void attachInstructor(Course course, User instructor) {
    courseInstructorRepository.save(
        CourseInstructor.builder()
            .course(course)
            .instructor(instructor)
            .role(InstructorRole.PRIMARY)
            .build());
  }

  protected CourseReview review(
      Course course, User student, int rating, String text, ReviewStatus status) {
    return courseReviewRepository.save(
        CourseReview.builder()
            .course(course)
            .user(student)
            .rating(rating)
            .reviewText(text)
            .status(status)
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
            .templateVersion(course.getTemplateVersion())
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
    Lesson lesson =
        lessonRepository.save(
            Lesson.builder()
                .section(section)
                .title("Lesson " + position)
                .slug(slug)
                .position(position)
                .status(status)
                .isFree(isFree)
                .lessonType(LessonType.VIDEO)
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
