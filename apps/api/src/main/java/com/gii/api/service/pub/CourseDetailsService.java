package com.gii.api.service.pub;

import com.gii.api.model.response.CategoryResponse;
import com.gii.api.model.response.CourseDetailsResponse;
import com.gii.api.model.response.CourseLiveClassSummaryResponse;
import com.gii.api.model.response.CourseQuizSummaryResponse;
import com.gii.api.model.response.CourseSectionItemResponse;
import com.gii.api.model.response.CourseSectionResponse;
import com.gii.api.model.response.InstructorSummaryResponse;
import com.gii.api.model.response.LessonSummaryResponse;
import com.gii.api.model.response.LessonVideoResponse;
import com.gii.api.model.response.lesson.LessonResourceSummaryResponse;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.common.entity.course.Category;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.LessonResource;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.course.SectionItem;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassSlot;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.user.User;
import com.gii.common.enums.LessonResourcePurpose;
import com.gii.common.enums.MediaProvider;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReviewStatus;
import com.gii.common.enums.SectionItemType;
import com.gii.common.repository.course.CourseCategoryRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseReviewRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.LessonResourceRepository;
import com.gii.common.repository.course.SectionItemRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.live.LiveClassSlotRepository;
import com.gii.common.repository.quiz.QuizQuestionRepository;
import com.gii.common.repository.quiz.QuizRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseDetailsService {

  private final CourseRepository courseRepository;
  private final CourseSectionRepository courseSectionRepository;
  private final LessonRepository lessonRepository;
  private final LessonResourceRepository lessonResourceRepository;
  private final CourseCategoryRepository courseCategoryRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final CourseReviewRepository courseReviewRepository;
  private final AssetUrlService assetUrlService;
  private final LocalizedContentService localizedContentService;
  private final SectionItemRepository sectionItemRepository;
  private final QuizRepository quizRepository;
  private final QuizQuestionRepository quizQuestionRepository;
  private final LiveClassSlotRepository liveClassSlotRepository;
  private final LiveClassRepository liveClassRepository;

  public CourseDetailsResponse execute(String slug) {
    Course course =
        courseRepository
            .findBySlugAndStatus(slug, PublishStatus.PUBLISHED)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

    List<CourseSection> sections =
        courseSectionRepository.findByCourseIdAndStatusOrderByPositionAsc(
            course.getId(), PublishStatus.PUBLISHED);

    List<Lesson> lessons =
        lessonRepository.findByCourseIdAndStatusWithMediaOrderByPositionAsc(
            course.getId(), PublishStatus.PUBLISHED);
    Map<UUID, List<Lesson>> lessonsBySectionId =
        lessons.stream()
            .filter(lesson -> lesson.getSection() != null)
            .collect(Collectors.groupingBy(lesson -> lesson.getSection().getId()));
    Map<UUID, Lesson> lessonById =
        lessons.stream().collect(Collectors.toMap(Lesson::getId, Function.identity()));
    Map<UUID, List<LessonResource>> resourcesByLessonId =
        lessonResourceRepository
            .findByLessonIdInOrderByLessonIdAscPositionAsc(
                lessons.stream().map(Lesson::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(resource -> resource.getLesson().getId()));

    List<UUID> sectionIds = sections.stream().map(CourseSection::getId).toList();
    Map<UUID, List<SectionItem>> itemsBySectionId =
        sectionIds.isEmpty()
            ? Map.of()
            : sectionItemRepository
                .findBySectionIdInOrderBySectionIdAscPositionAsc(sectionIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getSection().getId()));
    List<Quiz> quizzes =
        quizRepository.findByCourseIdAndStatusOrderByPositionAsc(
            course.getId(), PublishStatus.PUBLISHED);
    Map<UUID, Quiz> quizById =
        quizzes.stream().collect(Collectors.toMap(Quiz::getId, Function.identity()));
    Map<UUID, Long> questionCountByQuizId =
        quizzes.isEmpty()
            ? Map.of()
            : quizQuestionRepository.countByQuizIds(quizzes.stream().map(Quiz::getId).toList())
                .stream()
                .collect(
                    Collectors.toMap(
                        row -> (UUID) row[0], row -> ((Number) row[1]).longValue()));
    Map<UUID, LiveClassSlot> liveClassSlotById =
        sectionIds.isEmpty()
            ? Map.of()
            : liveClassSlotRepository.findBySectionIdIn(sectionIds).stream()
                .collect(Collectors.toMap(LiveClassSlot::getId, Function.identity()));
    Map<UUID, LiveClass> liveClassBySlotId =
        liveClassRepository.findByCourseId(course.getId()).stream()
            .collect(
                Collectors.toMap(
                    liveClass -> liveClass.getSlot().getId(),
                    Function.identity(),
                    (first, ignored) -> first));

    List<CourseSectionResponse> sectionResponses =
        sections.stream()
            .map(
                section ->
                    toSectionResponse(
                        section,
                        lessonsBySectionId.getOrDefault(section.getId(), List.of()),
                        resourcesByLessonId,
                        itemsBySectionId.getOrDefault(section.getId(), List.of()),
                        lessonById,
                        quizById,
                        questionCountByQuizId,
                        liveClassSlotById,
                        liveClassBySlotId))
            .toList();

    List<CategoryResponse> categoryResponses =
        courseCategoryRepository.findByCourseId(course.getId()).stream()
            .map(courseCategory -> toCategoryResponse(courseCategory.getCategory()))
            .filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing(CategoryResponse::name))
            .toList();

    List<InstructorSummaryResponse> instructors =
        courseInstructorRepository.findByCourseId(course.getId()).stream()
            .map(courseInstructor -> toInstructorSummaryResponse(courseInstructor.getInstructor()))
            .sorted(Comparator.comparing(InstructorSummaryResponse::fullName))
            .toList();

    List<Object[]> reviewAggregates =
        courseReviewRepository.aggregateByCourseIdAndStatus(course.getId(), ReviewStatus.PUBLISHED);
    Object[] reviewAggregate = reviewAggregates.isEmpty() ? null : reviewAggregates.getFirst();
    Double averageRating =
        reviewAggregate == null || reviewAggregate[0] == null
            ? null
            : ((Number) reviewAggregate[0]).doubleValue();
    long totalReviews =
        reviewAggregate == null || reviewAggregate[1] == null
            ? 0L
            : ((Number) reviewAggregate[1]).longValue();

    return CourseDetailsResponse.builder()
        .id(course.getId())
        .title(localizedContentService.text(course.getTitle(), course.getTitleEn()))
        .slug(course.getSlug())
        .shortDescription(
            localizedContentService.text(
                course.getShortDescription(), course.getShortDescriptionEn()))
        .description(
            localizedContentService.text(course.getDescription(), course.getDescriptionEn()))
        .language(course.getLanguage())
        .level(course.getLevel())
        .thumbnailUrl(assetUrlService.publicUrl(course.getThumbnailObjectKey()))
        .video(toCourseVideo(course.getYoutubeVideoId()))
        .priceBdt(course.getPriceBdt())
        .highlights(localizedContentService.list(course.getHighlights(), course.getHighlightsEn()))
        .courseOutcomes(
            localizedContentService.list(course.getCourseOutcomes(), course.getCourseOutcomesEn()))
        .requirements(
            localizedContentService.list(course.getRequirements(), course.getRequirementsEn()))
        .prerequisites(
            localizedContentService.list(course.getPrerequisites(), course.getPrerequisitesEn()))
        .studyMode(course.getStudyMode())
        .timezone(course.getTimezone())
        .enrollmentStartsAt(course.getEnrollmentStartsAt())
        .enrollmentEndsAt(course.getEnrollmentEndsAt())
        .startsAt(course.getStartsAt())
        .endsAt(course.getEndsAt())
        .capacity(course.getCapacity())
        .accessDurationDays(course.getAccessDurationDays())
        .categories(categoryResponses)
        .publishedAt(course.getPublishedAt())
        .instructors(instructors)
        .liveSessionCount(course.getLiveSessionCount())
        .quizCount(course.getQuizCount())
        .recordedHoursCount(course.getRecordedHoursCount())
        .isFree(course.getIsFree())
        .averageRating(averageRating)
        .totalReviews(totalReviews)
        .sections(sectionResponses)
        .build();
  }

  private LessonVideoResponse toCourseVideo(String youtubeVideoId) {
    return youtubeVideoId == null
        ? null
        : LessonVideoResponse.builder()
            .provider(MediaProvider.YOUTUBE)
            .sourceId(youtubeVideoId)
            .build();
  }

  private CourseSectionResponse toSectionResponse(
      CourseSection section,
      List<Lesson> lessonsForSection,
      Map<UUID, List<LessonResource>> resourcesByLessonId,
      List<SectionItem> orderedItems,
      Map<UUID, Lesson> lessonById,
      Map<UUID, Quiz> quizById,
      Map<UUID, Long> questionCountByQuizId,
      Map<UUID, LiveClassSlot> liveClassSlotById,
      Map<UUID, LiveClass> liveClassBySlotId) {
    Map<UUID, Integer> positions =
        orderedItems.stream()
            .collect(Collectors.toMap(SectionItem::getItemId, SectionItem::getPosition));
    List<LessonSummaryResponse> lessons =
        lessonsForSection.stream()
            .map(
                lesson ->
                    toLessonSummaryResponse(
                        lesson,
                        positions.get(lesson.getId()),
                        resourcesByLessonId.getOrDefault(lesson.getId(), List.of())))
            .toList();
    List<CourseSectionItemResponse> items =
        orderedItems.stream()
            .map(
                item ->
                    toSectionItemResponse(
                        item,
                        lessonById,
                        resourcesByLessonId,
                        quizById,
                        questionCountByQuizId,
                        liveClassSlotById,
                        liveClassBySlotId))
            .filter(java.util.Objects::nonNull)
            .toList();

    return CourseSectionResponse.builder()
        .id(section.getId())
        .title(localizedContentService.text(section.getTitle(), section.getTitleEn()))
        .description(
            localizedContentService.text(section.getDescription(), section.getDescriptionEn()))
        .position(section.getPosition())
        .items(items)
        .lessons(lessons)
        .build();
  }

  private CourseSectionItemResponse toSectionItemResponse(
      SectionItem item,
      Map<UUID, Lesson> lessonById,
      Map<UUID, List<LessonResource>> resourcesByLessonId,
      Map<UUID, Quiz> quizById,
      Map<UUID, Long> questionCountByQuizId,
      Map<UUID, LiveClassSlot> liveClassSlotById,
      Map<UUID, LiveClass> liveClassBySlotId) {
    if (item.getItemType() == SectionItemType.LESSON) {
      Lesson lesson = lessonById.get(item.getItemId());
      if (lesson == null) {
        return null;
      }
      return CourseSectionItemResponse.builder()
          .itemId(item.getItemId())
          .itemType(item.getItemType())
          .position(item.getPosition())
          .lesson(
              toLessonSummaryResponse(
                  lesson,
                  item.getPosition(),
                  resourcesByLessonId.getOrDefault(lesson.getId(), List.of())))
          .build();
    }
    if (item.getItemType() == SectionItemType.QUIZ) {
      Quiz quiz = quizById.get(item.getItemId());
      if (quiz == null) {
        return null;
      }
      return CourseSectionItemResponse.builder()
          .itemId(item.getItemId())
          .itemType(item.getItemType())
          .position(item.getPosition())
          .quiz(toQuizSummaryResponse(quiz, questionCountByQuizId.getOrDefault(quiz.getId(), 0L)))
          .build();
    }
    if (item.getItemType() == SectionItemType.LIVE_CLASS) {
      LiveClassSlot slot = liveClassSlotById.get(item.getItemId());
      if (slot == null) {
        return null;
      }
      return CourseSectionItemResponse.builder()
          .itemId(item.getItemId())
          .itemType(item.getItemType())
          .position(item.getPosition())
          .liveClass(toLiveClassSummaryResponse(slot, liveClassBySlotId.get(slot.getId())))
          .build();
    }
    return null;
  }

  private CourseQuizSummaryResponse toQuizSummaryResponse(Quiz quiz, long questionCount) {
    return CourseQuizSummaryResponse.builder()
        .id(quiz.getId())
        .title(localizedContentService.text(quiz.getTitle(), quiz.getTitleEn()))
        .questionCount(questionCount)
        .passingScorePct(quiz.getPassingScorePct())
        .maxAttempts(quiz.getMaxAttempts())
        .timeLimitSec(quiz.getTimeLimitSec())
        .build();
  }

  private CourseLiveClassSummaryResponse toLiveClassSummaryResponse(
      LiveClassSlot slot, LiveClass liveClass) {
    return CourseLiveClassSummaryResponse.builder()
        .id(slot.getId())
        .title(
            localizedContentService.text(
                liveClass == null ? slot.getTitle() : liveClass.getTitle(),
                liveClass == null ? slot.getTitleEn() : liveClass.getTitleEn()))
        .description(
            localizedContentService.text(
                liveClass == null ? slot.getDescription() : liveClass.getDescription(),
                liveClass == null ? slot.getDescriptionEn() : liveClass.getDescriptionEn()))
        .expectedDurationMinutes(slot.getExpectedDurationMinutes())
        .isMandatory(slot.getIsMandatory())
        .scheduled(liveClass != null)
        .startsAt(liveClass == null ? null : liveClass.getStartsAt())
        .endsAt(liveClass == null ? null : liveClass.getEndsAt())
        .provider(liveClass == null ? null : liveClass.getProvider())
        .status(liveClass == null ? null : liveClass.getStatus())
        .build();
  }

  private LessonSummaryResponse toLessonSummaryResponse(
      Lesson lesson, Integer position, List<LessonResource> lessonResources) {
    MediaAsset media = lesson.getPrimaryMediaAsset();

    LessonVideoResponse video = null;

    if (Boolean.TRUE.equals(lesson.getIsFree()) && media != null) {
      video =
          LessonVideoResponse.builder()
              .provider(media.getProvider())
              .sourceId(media.getProviderAssetId()) // YouTube ID
              .build();
    }

    List<LessonResourceSummaryResponse> resourceSummaries =
        lessonResources.stream().map(this::toResourceSummary).toList();
    LessonResourceSummaryResponse primaryResource =
        resourceSummaries.stream()
            .filter(resource -> resource.purpose() == LessonResourcePurpose.PRIMARY_CONTENT)
            .findFirst()
            .orElse(null);
    List<LessonResourceSummaryResponse> supplementaryResources =
        resourceSummaries.stream()
            .filter(resource -> resource.purpose() == LessonResourcePurpose.SUPPLEMENTARY)
            .toList();

    return LessonSummaryResponse.builder()
        .id(lesson.getId())
        .title(localizedContentService.text(lesson.getTitle(), lesson.getTitleEn()))
        .slug(lesson.getSlug())
        .position(position)
        .lessonType(lesson.getLessonType())
        .isPreviewFree(lesson.getIsFree())
        .durationSeconds(lesson.getDurationSeconds())
        .video(video)
        .primaryResource(primaryResource)
        .resources(supplementaryResources)
        .build();
  }

  private LessonResourceSummaryResponse toResourceSummary(LessonResource resource) {
    return LessonResourceSummaryResponse.builder()
        .resourceId(resource.getId())
        .title(localizedContentService.text(resource.getTitle(), resource.getTitleEn()))
        .resourceType(resource.getResourceType())
        .purpose(resource.getPurpose())
        .mimeType(resource.getMimeType())
        .position(resource.getPosition())
        .build();
  }

  private CategoryResponse toCategoryResponse(Category category) {
    if (category == null) {
      return null;
    }

    return CategoryResponse.builder()
        .id(category.getId())
        .name(localizedContentService.text(category.getName(), category.getNameEn()))
        .slug(category.getSlug())
        .parentId(category.getParent() != null ? category.getParent().getId() : null)
        .build();
  }

  private InstructorSummaryResponse toInstructorSummaryResponse(User instructor) {
    return InstructorSummaryResponse.builder()
        .id(instructor.getId())
        .fullName(instructor.getFullName())
        .avatarUrl(null)
        .shortBio(null)
        .credentials(null)
        .build();
  }
}
