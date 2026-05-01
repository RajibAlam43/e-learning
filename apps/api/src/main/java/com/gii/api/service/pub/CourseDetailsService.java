package com.gii.api.service.pub;

import com.gii.api.model.response.CategoryResponse;
import com.gii.api.model.response.CourseDetailsResponse;
import com.gii.api.model.response.CourseSectionResponse;
import com.gii.api.model.response.InstructorSummaryResponse;
import com.gii.api.model.response.LessonSummaryResponse;
import com.gii.api.model.response.LessonVideoResponse;
import com.gii.common.entity.course.Category;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.user.User;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseCategoryRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  private final CourseCategoryRepository courseCategoryRepository;
  private final CourseInstructorRepository courseInstructorRepository;

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

    List<CourseSectionResponse> sectionResponses =
        sections.stream()
            .map(
                section ->
                    toSectionResponse(
                        section, lessonsBySectionId.getOrDefault(section.getId(), List.of())))
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

    return CourseDetailsResponse.builder()
        .id(course.getId())
        .title(course.getTitle())
        .slug(course.getSlug())
        .shortDescription(course.getShortDescription())
        .description(course.getDescription())
        .language(course.getLanguage())
        .level(course.getLevel())
        .thumbnailUrl(course.getThumbnailUrl())
        .priceBdt(course.getPriceBdt())
        .highlights(course.getHighlights())
        .courseOutcomes(course.getCourseOutcomes())
        .requirements(course.getRequirements())
        .prerequisites(course.getPrerequisites())
        .studyMode(course.getStudyMode())
        .categories(categoryResponses)
        .publishedAt(course.getPublishedAt())
        .instructors(instructors)
        .liveSessionCount(course.getLiveSessionCount())
        .quizCount(course.getQuizCount())
        .recordedHoursCount(course.getRecordedHoursCount())
        .isFree(course.getIsFree())
        .sections(sectionResponses)
        .build();
  }

  private CourseSectionResponse toSectionResponse(
      CourseSection section, List<Lesson> lessonsForSection) {
    List<LessonSummaryResponse> lessons =
        lessonsForSection.stream().map(this::toLessonSummaryResponse).toList();

    return CourseSectionResponse.builder()
        .id(section.getId())
        .title(section.getTitle())
        .position(section.getPosition())
        .lessons(lessons)
        .build();
  }

  private LessonSummaryResponse toLessonSummaryResponse(Lesson lesson) {
    MediaAsset media = lesson.getPrimaryMediaAsset();

    LessonVideoResponse video = null;

    if (Boolean.TRUE.equals(lesson.getIsFree()) && media != null) {
      video =
          LessonVideoResponse.builder()
              .provider(media.getProvider())
              .sourceId(media.getProviderAssetId()) // YouTube ID
              .build();
    }

    return LessonSummaryResponse.builder()
        .id(lesson.getId())
        .title(lesson.getTitle())
        .slug(lesson.getSlug())
        .position(lesson.getPosition())
        .lessonType(lesson.getLessonType())
        .isPreviewFree(lesson.getIsFree())
        .video(video)
        .build();
  }

  private CategoryResponse toCategoryResponse(Category category) {
    if (category == null) {
      return null;
    }

    return CategoryResponse.builder()
        .id(category.getId())
        .name(category.getName())
        .slug(category.getSlug())
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
