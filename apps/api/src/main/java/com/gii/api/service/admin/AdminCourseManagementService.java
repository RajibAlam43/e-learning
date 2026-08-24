package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateCourseRequest;
import com.gii.api.model.request.admin.FeatureCourseRequest;
import com.gii.api.model.request.admin.ReorderCourseStructureRequest;
import com.gii.api.model.request.admin.RepeatCourseRequest;
import com.gii.api.model.request.admin.UpdateCourseRequest;
import com.gii.api.model.response.admin.AdminCategoryResponse;
import com.gii.api.model.response.admin.AdminCourseDetailResponse;
import com.gii.api.model.response.admin.AdminCourseSectionResponse;
import com.gii.api.model.response.admin.AdminCourseSummaryResponse;
import com.gii.api.model.response.admin.AdminInstructorSummaryResponse;
import com.gii.api.service.course.CourseDuplicationService;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.common.entity.course.Category;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseCategory;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.CourseTemplate;
import com.gii.common.entity.course.SectionItem;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.SectionItemType;
import com.gii.common.enums.StudyMode;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.course.CategoryRepository;
import com.gii.common.repository.course.CourseCategoryRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.CourseTemplateRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.SectionItemRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.quiz.QuizRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCourseManagementService {

  private final CourseRepository courseRepository;
  private final CourseTemplateRepository courseTemplateRepository;
  private final CategoryRepository categoryRepository;
  private final CourseCategoryRepository courseCategoryRepository;
  private final CourseSectionRepository sectionRepository;
  private final LessonRepository lessonRepository;
  private final QuizRepository quizRepository;
  private final SectionItemRepository sectionItemRepository;
  private final CourseInstructorRepository instructorRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final LiveClassRepository liveClassRepository;
  private final CollectionCourseRepository collectionCourseRepository;
  private final CurrentUserService currentUserService;
  private final AdminSectionManagementService sectionManagementService;
  private final AssetUrlService assetUrlService;
  private final CourseDuplicationService courseDuplicationService;

  @Transactional(readOnly = true)
  public List<AdminCourseSummaryResponse> list() {
    List<Course> courses = courseRepository.findAll();
    List<UUID> courseIds = courses.stream().map(Course::getId).toList();
    Map<UUID, String> instructorNameByCourseId = buildInstructorNameMap(courseIds);
    Map<UUID, Integer> activeEnrollmentCountByCourseId =
        courseIds.isEmpty()
            ? Map.of()
            : toCountMap(
                enrollmentRepository.countByCourseIdsAndStatus(courseIds, EnrollmentStatus.ACTIVE));

    return courses.stream()
        .map(
            course -> {
              return AdminCourseSummaryResponse.builder()
                  .courseId(course.getId())
                  .title(course.getTitle())
                  .slug(course.getSlug())
                  .thumbnailUrl(assetUrlService.publicUrl(course.getThumbnailObjectKey()))
                  .status(course.getStatus())
                  .priceBdt(course.getPriceBdt())
                  .isFree(course.getIsFree())
                  .instructorName(instructorNameByCourseId.get(course.getId()))
                  .totalEnrolled(activeEnrollmentCountByCourseId.getOrDefault(course.getId(), 0))
                  .isFeatured(course.getIsFeatured())
                  .featuredPosition(course.getFeaturedPosition())
                  .featuredAt(course.getFeaturedAt())
                  .publishedAt(course.getPublishedAt())
                  .createdAt(course.getCreatedAt())
                  .build();
            })
        .toList();
  }

  public AdminCourseDetailResponse create(
      CreateCourseRequest request, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    CourseTemplate template =
        courseTemplateRepository.save(
            CourseTemplate.builder()
                .title(request.title().trim())
                .titleEn(request.titleEn())
                .thumbnailObjectKey(
                    assetUrlService.normalizeThumbnailKey(request.thumbnailObjectKey(), "courses"))
                .shortDescription(request.shortDescription())
                .shortDescriptionEn(request.shortDescriptionEn())
                .description(request.description())
                .descriptionEn(request.descriptionEn())
                .highlights(request.highlights())
                .highlightsEn(request.highlightsEn())
                .courseOutcomes(request.courseOutcomes())
                .courseOutcomesEn(request.courseOutcomesEn())
                .requirements(request.requirements())
                .requirementsEn(request.requirementsEn())
                .prerequisites(toList(request.prerequisites()))
                .prerequisitesEn(toList(request.prerequisitesEn()))
                .level(request.level())
                .language(request.language())
                .estimatedDurationMinutes(request.estimatedDurationMinutes())
                .targetAudience(request.targetAudience())
                .targetAudienceEn(request.targetAudienceEn())
                .liveSessionCount(0)
                .quizCount(0)
                .recordedHoursCount(0)
                .build());
    Course course =
        Course.builder()
            .template(template)
            .slug(request.slug().trim())
            .name(request.title().trim())
            .priceBdt(request.priceBdt())
            .studyMode(request.studyMode())
            .status(PublishStatus.DRAFT)
            .isFree(Boolean.TRUE.equals(request.isFree()))
            .timezone(normalizeTimezone(request.timezone()))
            .enrollmentStartsAt(request.enrollmentStartsAt())
            .enrollmentEndsAt(request.enrollmentEndsAt())
            .startsAt(request.startsAt())
            .endsAt(request.endsAt())
            .capacity(request.capacity())
            .accessDurationDays(request.accessDurationDays())
            .createdBy(user)
            .build();
    validateOfferingWindow(course);
    Course savedCourse = courseRepository.save(course);
    replaceCategories(savedCourse, request.categoryIds());
    return getResponse(savedCourse);
  }

  public AdminCourseDetailResponse repeat(
      UUID sourceCourseId, RepeatCourseRequest request, Authentication authentication) {
    Course source = findCourse(sourceCourseId);
    User user = currentUserService.getCurrentUser(authentication);
    Course course = courseDuplicationService.duplicate(source, request.slug().trim(), user);
    course.setPriceBdt(request.priceBdt());
    course.setStudyMode(request.studyMode());
    course.setIsFree(Boolean.TRUE.equals(request.isFree()));
    course.setTimezone(normalizeTimezone(request.timezone()));
    course.setEnrollmentStartsAt(request.enrollmentStartsAt());
    course.setEnrollmentEndsAt(request.enrollmentEndsAt());
    course.setStartsAt(request.startsAt());
    course.setEndsAt(request.endsAt());
    course.setCapacity(request.capacity());
    course.setAccessDurationDays(request.accessDurationDays());
    validateOfferingWindow(course);
    return getResponse(courseRepository.save(course));
  }

  @Transactional(readOnly = true)
  public AdminCourseDetailResponse get(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    return getResponse(course);
  }

  public AdminCourseDetailResponse update(UUID courseId, UpdateCourseRequest request) {
    Course course =
        courseRepository
            .findByIdForUpdate(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    if (request.getTitle() != null) {
      course.setTitle(request.getTitle().trim());
    }
    if (request.getTitleEn() != null) {
      course.setTitleEn(request.getTitleEn().trim());
    }
    if (request.getSlug() != null) {
      course.setSlug(request.getSlug().trim());
    }
    if (request.getCategoryIds() != null) {
      replaceCategories(course, request.getCategoryIds());
    }
    if (request.getThumbnailObjectKey() != null) {
      course.setThumbnailObjectKey(
          assetUrlService.normalizeThumbnailKey(request.getThumbnailObjectKey(), "courses"));
    }
    if (request.getShortDescription() != null) {
      course.setShortDescription(request.getShortDescription());
    }
    if (request.getShortDescriptionEn() != null) {
      course.setShortDescriptionEn(request.getShortDescriptionEn());
    }
    if (request.getDescription() != null) {
      course.setDescription(request.getDescription());
    }
    if (request.getDescriptionEn() != null) {
      course.setDescriptionEn(request.getDescriptionEn());
    }
    if (request.getHighlights() != null) {
      course.setHighlights(request.getHighlights());
    }
    if (request.getHighlightsEn() != null) {
      course.setHighlightsEn(request.getHighlightsEn());
    }
    if (request.getPriceBdt() != null) {
      course.setPriceBdt(request.getPriceBdt());
    }
    if (request.getCourseOutcomes() != null) {
      course.setCourseOutcomes(request.getCourseOutcomes());
    }
    if (request.getCourseOutcomesEn() != null) {
      course.setCourseOutcomesEn(request.getCourseOutcomesEn());
    }
    if (request.getRequirements() != null) {
      course.setRequirements(request.getRequirements());
    }
    if (request.getRequirementsEn() != null) {
      course.setRequirementsEn(request.getRequirementsEn());
    }
    if (request.getPrerequisites() != null) {
      course.setPrerequisites(toList(request.getPrerequisites()));
    }
    if (request.getPrerequisitesEn() != null) {
      course.setPrerequisitesEn(toList(request.getPrerequisitesEn()));
    }
    if (request.getLevel() != null) {
      course.setLevel(CourseLevel.valueOf(request.getLevel().toUpperCase()));
    }
    if (request.getLanguage() != null) {
      course.setLanguage(CourseLanguage.valueOf(request.getLanguage().toUpperCase()));
    }
    if (request.getStudyMode() != null) {
      course.setStudyMode(StudyMode.valueOf(request.getStudyMode().toUpperCase()));
    }
    if (request.getIsFree() != null) {
      course.setIsFree(request.getIsFree());
    }
    if (request.getEstimatedDurationMinutes() != null) {
      course.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
    }
    if (request.getTargetAudience() != null) {
      course.setTargetAudience(request.getTargetAudience());
    }
    if (request.getTargetAudienceEn() != null) {
      course.setTargetAudienceEn(request.getTargetAudienceEn());
    }
    if (request.isTimezonePresent()) {
      course.setTimezone(normalizeTimezone(request.getTimezone()));
    }
    if (request.isEnrollmentStartsAtPresent()) {
      course.setEnrollmentStartsAt(request.getEnrollmentStartsAt());
    }
    if (request.isEnrollmentEndsAtPresent()) {
      course.setEnrollmentEndsAt(request.getEnrollmentEndsAt());
    }
    if (request.isStartsAtPresent()) {
      course.setStartsAt(request.getStartsAt());
    }
    if (request.isEndsAtPresent()) {
      course.setEndsAt(request.getEndsAt());
    }
    if (request.isCapacityPresent()) {
      course.setCapacity(request.getCapacity());
    }
    if (request.isAccessDurationDaysPresent()) {
      course.setAccessDurationDays(request.getAccessDurationDays());
    }
    validateOfferingWindow(course);
    validateScheduledLiveClassesWithinOffering(course);
    return getResponse(courseRepository.save(course));
  }

  public void publish(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    List<CourseSection> sections = sectionRepository.findByCourseIdOrderByPositionAsc(courseId);
    if (sections.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Course must have at least one section before publishing");
    }
    if (lessonRepository.findByCourseIdOrderByPositionAsc(courseId).isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Course must have at least one lesson before publishing");
    }
    course.setStatus(PublishStatus.PUBLISHED);
    course.setPublishedAt(Instant.now());
    courseRepository.save(course);
  }

  public void unpublish(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    if (collectionCourseRepository.existsByCourseIdAndCollectionStatus(
        courseId, PublishStatus.PUBLISHED)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Course belongs to a published collection");
    }
    course.setStatus(PublishStatus.DRAFT);
    clearFeatured(course);
    courseRepository.save(course);
  }

  public AdminCourseDetailResponse feature(UUID courseId, FeatureCourseRequest request) {
    Course course = findCourse(courseId);
    if (course.getStatus() != PublishStatus.PUBLISHED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only published courses can be featured");
    }
    course.setIsFeatured(true);
    course.setFeaturedPosition(request.position());
    course.setFeaturedAt(Instant.now());
    return getResponse(courseRepository.save(course));
  }

  public void unfeature(UUID courseId) {
    Course course = findCourse(courseId);
    clearFeatured(course);
    courseRepository.save(course);
  }

  private Course findCourse(UUID courseId) {
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
  }

  private void clearFeatured(Course course) {
    course.setIsFeatured(false);
    course.setFeaturedPosition(null);
    course.setFeaturedAt(null);
  }

  public void reorder(UUID courseId, ReorderCourseStructureRequest request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    try {
      Set<UUID> seenSectionIds = new HashSet<>();
      Set<Integer> seenSectionPositions = new HashSet<>();
      List<CourseSection> sectionsToReposition = new java.util.ArrayList<>();
      Map<UUID, Integer> targetSectionPositions = new LinkedHashMap<>();
      for (var secReq : request.sections()) {
        if (secReq.sectionId() == null
            || secReq.newPosition() == null
            || secReq.newPosition() <= 0) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Invalid section reorder entry");
        }
        if (!seenSectionIds.add(secReq.sectionId())
            || !seenSectionPositions.add(secReq.newPosition())) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Duplicate section id or position");
        }
        CourseSection sec =
            sectionRepository
                .findById(secReq.sectionId())
                .orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        if (!sec.getTemplate().getId().equals(course.getTemplate().getId())) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Section does not belong to course");
        }
        sectionsToReposition.add(sec);
        targetSectionPositions.put(sec.getId(), secReq.newPosition());
      }
      int temporarySectionPosition = 2000000;
      for (int i = 0; i < sectionsToReposition.size(); i++) {
        sectionsToReposition.get(i).setPosition(temporarySectionPosition + i);
      }
      sectionRepository.saveAllAndFlush(sectionsToReposition);
      for (CourseSection section : sectionsToReposition) {
        section.setPosition(targetSectionPositions.get(section.getId()));
      }
      sectionRepository.saveAllAndFlush(sectionsToReposition);

      for (var secReq : request.sections()) {
        CourseSection sec =
            sectionsToReposition.stream()
                .filter(section -> section.getId().equals(secReq.sectionId()))
                .findFirst()
                .orElseThrow();
        if (secReq.items() == null || secReq.items().isEmpty()) {
          continue;
        }
        applyItemReorder(secReq, sec);
      }
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid reorder payload or duplicate positions");
    }
  }

  private void applyItemReorder(
      com.gii.api.model.request.admin.SectionReorderRequest secReq, CourseSection sec) {
    Set<UUID> seenItemIds = new HashSet<>();
    Set<Integer> seenPositions = new HashSet<>();
    List<SectionItem> itemsToReposition = new java.util.ArrayList<>();
    Map<UUID, Integer> targetPositionByItemId = new LinkedHashMap<>();
    for (var itemReq : secReq.items()) {
      if (itemReq.itemId() == null || itemReq.itemType() == null || itemReq.newPosition() == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Invalid section item reorder entry");
      }
      if (itemReq.newPosition() <= 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item position must be positive");
      }
      if (!seenItemIds.add(itemReq.itemId()) || !seenPositions.add(itemReq.newPosition())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Duplicate item id or position in section reorder request");
      }
      SectionItemType itemType = itemReq.itemType();
      UUID persistedItemId = resolvePersistedSectionItemId(itemType, itemReq.itemId());
      targetPositionByItemId.put(persistedItemId, itemReq.newPosition());
      SectionItem sectionItem =
          sectionItemRepository
              .findByItemTypeAndItemId(itemType, persistedItemId)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(HttpStatus.NOT_FOUND, "Section item not found"));
      if (!sectionItem.getSection().getId().equals(sec.getId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Section item does not belong to section");
      }
      itemsToReposition.add(sectionItem);
    }

    int tempBase = 1000000;
    for (int i = 0; i < itemsToReposition.size(); i++) {
      SectionItem item = itemsToReposition.get(i);
      item.setPosition(tempBase + i);
    }
    sectionItemRepository.saveAllAndFlush(itemsToReposition);

    for (SectionItem item : itemsToReposition) {
      Integer finalPosition = targetPositionByItemId.get(item.getItemId());
      if (finalPosition == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section item missing");
      }
      item.setPosition(finalPosition);
    }
    sectionItemRepository.saveAllAndFlush(itemsToReposition);
  }

  private UUID resolvePersistedSectionItemId(SectionItemType itemType, UUID apiItemId) {
    if (itemType != SectionItemType.LIVE_CLASS) {
      return apiItemId;
    }
    return liveClassRepository
        .findById(apiItemId)
        .map(liveClass -> liveClass.getSlot().getId())
        .orElse(apiItemId);
  }

  private AdminCourseDetailResponse getResponse(Course course) {
    List<AdminCategoryResponse> categories =
        courseCategoryRepository.findByCourseId(course.getId()).stream()
            .map(CourseCategory::getCategory)
            .map(this::toCategoryResponse)
            .sorted(
                Comparator.comparing(AdminCategoryResponse::name, String.CASE_INSENSITIVE_ORDER))
            .toList();

    List<AdminCourseSectionResponse> sections =
        sectionRepository.findByCourseIdOrderByPositionAsc(course.getId()).stream()
            .map(section -> sectionManagementService.toResponse(section, course.getId()))
            .toList();

    List<AdminInstructorSummaryResponse> instructors =
        instructorRepository.findByCourseId(course.getId()).stream()
            .map(CourseInstructor::getInstructor)
            .map(
                i ->
                    AdminInstructorSummaryResponse.builder()
                        .userId(i.getId())
                        .fullName(i.getFullName())
                        .email(i.getEmail())
                        .displayName(i.getFullName())
                        .headline(null)
                        .isPublic(true)
                        .assignedCoursesCount(0)
                        .createdAt(i.getCreatedAt())
                        .build())
            .toList();

    return AdminCourseDetailResponse.builder()
        .courseId(course.getId())
        .title(course.getTitle())
        .titleEn(course.getTitleEn())
        .slug(course.getSlug())
        .categories(categories)
        .thumbnailObjectKey(course.getThumbnailObjectKey())
        .thumbnailUrl(assetUrlService.publicUrl(course.getThumbnailObjectKey()))
        .shortDescription(course.getShortDescription())
        .shortDescriptionEn(course.getShortDescriptionEn())
        .description(course.getDescription())
        .descriptionEn(course.getDescriptionEn())
        .highlights(course.getHighlights())
        .highlightsEn(course.getHighlightsEn())
        .priceBdt(course.getPriceBdt())
        .courseOutcomes(course.getCourseOutcomes())
        .courseOutcomesEn(course.getCourseOutcomesEn())
        .requirements(course.getRequirements())
        .requirementsEn(course.getRequirementsEn())
        .level(course.getLevel())
        .language(course.getLanguage())
        .studyMode(course.getStudyMode())
        .status(course.getStatus())
        .isFeatured(course.getIsFeatured())
        .featuredPosition(course.getFeaturedPosition())
        .featuredAt(course.getFeaturedAt())
        .isFree(course.getIsFree())
        .liveSessionCount(course.getLiveSessionCount())
        .quizCount(course.getQuizCount())
        .recordedHoursCount(course.getRecordedHoursCount())
        .estimatedDurationMinutes(course.getEstimatedDurationMinutes())
        .targetAudience(course.getTargetAudience())
        .targetAudienceEn(course.getTargetAudienceEn())
        .prerequisites(
            course.getPrerequisites() != null ? String.join(", ", course.getPrerequisites()) : null)
        .prerequisitesEn(
            course.getPrerequisitesEn() != null
                ? String.join(", ", course.getPrerequisitesEn())
                : null)
        .timezone(course.getTimezone())
        .enrollmentStartsAt(course.getEnrollmentStartsAt())
        .enrollmentEndsAt(course.getEnrollmentEndsAt())
        .startsAt(course.getStartsAt())
        .endsAt(course.getEndsAt())
        .capacity(course.getCapacity())
        .accessDurationDays(course.getAccessDurationDays())
        .createdBy(course.getCreatedBy() != null ? course.getCreatedBy().getId() : null)
        .publishedAt(course.getPublishedAt())
        .createdAt(course.getCreatedAt())
        .updatedAt(course.getUpdatedAt())
        .sections(sections)
        .instructors(instructors)
        .build();
  }

  private String normalizeTimezone(String timezone) {
    return timezone == null || timezone.isBlank() ? null : timezone.trim();
  }

  private void validateOfferingWindow(Course course) {
    if (course.getEnrollmentStartsAt() != null
        && course.getEnrollmentEndsAt() != null
        && !course.getEnrollmentEndsAt().isAfter(course.getEnrollmentStartsAt())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Enrollment end must be after enrollment start");
    }
    if (course.getStartsAt() != null
        && course.getEndsAt() != null
        && !course.getEndsAt().isAfter(course.getStartsAt())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course end must be after start");
    }
  }

  private void validateScheduledLiveClassesWithinOffering(Course course) {
    for (var liveClass : liveClassRepository.findByCourseId(course.getId())) {
      if (course.getStartsAt() != null && liveClass.getStartsAt().isBefore(course.getStartsAt())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Course start cannot be after an existing live class start");
      }
      if (course.getEndsAt() != null && liveClass.getEndsAt().isAfter(course.getEndsAt())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Course end cannot be before an existing live class end");
      }
    }
  }

  private void replaceCategories(Course course, List<UUID> categoryIds) {
    if (categoryIds == null || categoryIds.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Course must have at least one category");
    }
    Set<UUID> uniqueCategoryIds = new HashSet<>(categoryIds);
    if (uniqueCategoryIds.size() != categoryIds.size()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Duplicate categories are not allowed");
    }
    List<Category> categories = categoryRepository.findAllById(uniqueCategoryIds);
    if (categories.size() != uniqueCategoryIds.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more categories not found");
    }

    List<CourseCategory> existing = courseCategoryRepository.findByCourseId(course.getId());
    if (!existing.isEmpty()) {
      courseCategoryRepository.deleteAllInBatch(existing);
    }
    courseCategoryRepository.saveAll(
        categories.stream()
            .map(
                category ->
                    CourseCategory.builder().template(course.getTemplate()).category(category).build())
            .toList());
  }

  private AdminCategoryResponse toCategoryResponse(Category category) {
    return AdminCategoryResponse.builder()
        .id(category.getId())
        .name(category.getName())
        .nameEn(category.getNameEn())
        .slug(category.getSlug())
        .parentId(category.getParent() != null ? category.getParent().getId() : null)
        .createdAt(category.getCreatedAt())
        .build();
  }

  private List<String> toList(String commaSeparated) {
    if (commaSeparated == null || commaSeparated.isBlank()) {
      return List.of();
    }
    return List.of(commaSeparated.split(",")).stream()
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();
  }

  private Map<UUID, String> buildInstructorNameMap(List<UUID> courseIds) {
    if (courseIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, String> result = new HashMap<>();
    for (CourseInstructor instructor : instructorRepository.findByCourseIds(courseIds)) {
      UUID courseId = instructor.getCourse().getId();
      String fullName = instructor.getInstructor().getFullName();
      if (!result.containsKey(courseId) || instructor.getRole() == InstructorRole.PRIMARY) {
        result.put(courseId, fullName);
      }
    }
    return result;
  }

  private Map<UUID, Integer> toCountMap(List<Object[]> rows) {
    Map<UUID, Integer> result = new HashMap<>();
    for (Object[] row : rows) {
      result.put((UUID) row[0], ((Long) row[1]).intValue());
    }
    return result;
  }
}
