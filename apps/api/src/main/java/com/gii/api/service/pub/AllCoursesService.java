package com.gii.api.service.pub;

import com.gii.api.model.response.CourseSummaryResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseCategory;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseCategoryRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllCoursesService {

  private static final int MAX_PAGE_SIZE = 20;
  private static final String DEFAULT_SORT_FIELD = "publishedAt";
  private static final List<String> ALLOWED_SORT_FIELDS =
      List.of("publishedAt", "priceBdt", "title");

  private final CourseRepository courseRepository;
  private final CourseCategoryRepository courseCategoryRepository;
  private final CourseInstructorRepository courseInstructorRepository;

  public PageResponse<CourseSummaryResponse> execute(
      UUID categoryId, CourseLevel level, CourseLanguage language, Pageable pageable) {
    Pageable safePageable = createSafePageable(pageable);

    Specification<Course> spec =
        Specification.where(CourseSpecifications.hasStatus(PublishStatus.PUBLISHED))
            .and(CourseSpecifications.hasCategory(categoryId))
            .and(CourseSpecifications.hasLevel(level))
            .and(CourseSpecifications.hasLanguage(language));

    Page<Course> coursePage = courseRepository.findAll(spec, safePageable);
    List<Course> courseList = coursePage.getContent();
    List<UUID> courseIds = courseList.stream().map(Course::getId).toList();

    Map<UUID, List<String>> categoryNamesByCourseId = getCategoryNamesByCourseId(courseIds);
    Map<UUID, List<String>> instructorNamesByCourseId = getInstructorNamesByCourseId(courseIds);

    List<CourseSummaryResponse> courses =
        courseList.stream()
            .map(
                course ->
                    toCourseSummaryResponse(
                        course,
                        categoryNamesByCourseId.getOrDefault(course.getId(), List.of()),
                        instructorNamesByCourseId.getOrDefault(course.getId(), List.of())))
            .toList();

    return PageResponse.<CourseSummaryResponse>builder()
        .content(courses)
        .page(coursePage.getNumber())
        .size(coursePage.getSize())
        .totalElements(coursePage.getTotalElements())
        .totalPages(coursePage.getTotalPages())
        .build();
  }

  private Pageable createSafePageable(Pageable pageable) {
    int pageNumber = Math.max(pageable.getPageNumber(), 0);
    int pageSize = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
    Sort safeSort = sanitizeSort(pageable.getSort());
    return PageRequest.of(pageNumber, pageSize, safeSort);
  }

  private Sort sanitizeSort(Sort requestedSort) {
    Sort defaultSort = Sort.by(Sort.Order.desc(DEFAULT_SORT_FIELD), Sort.Order.desc("id"));

    if (requestedSort == null || requestedSort.isUnsorted()) {
      return defaultSort;
    }

    List<Sort.Order> safeOrders =
        requestedSort.stream()
            .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
            .toList();

    if (safeOrders.isEmpty()) {
      return defaultSort;
    }

    if (safeOrders.stream().noneMatch(order -> order.getProperty().equals("id"))) {
      safeOrders = new java.util.ArrayList<>(safeOrders);
      safeOrders.add(Sort.Order.desc("id"));
    }

    return Sort.by(safeOrders);
  }

  private Map<UUID, List<String>> getCategoryNamesByCourseId(List<UUID> courseIds) {
    if (courseIds.isEmpty()) {
      return Map.of();
    }

    List<CourseCategory> courseCategories = courseCategoryRepository.findByCourseIds(courseIds);
    Map<UUID, List<String>> result = new HashMap<>();
    for (CourseCategory courseCategory : courseCategories) {
      result
          .computeIfAbsent(
              courseCategory.getCourse().getId(), ignored -> new java.util.ArrayList<>())
          .add(courseCategory.getCategory().getName());
    }
    result.replaceAll((key, value) -> new java.util.ArrayList<>(new LinkedHashSet<>(value)));
    return result;
  }

  private Map<UUID, List<String>> getInstructorNamesByCourseId(List<UUID> courseIds) {
    if (courseIds.isEmpty()) {
      return Map.of();
    }

    List<CourseInstructor> instructors = courseInstructorRepository.findByCourseIds(courseIds);
    return instructors.stream()
        .collect(
            Collectors.groupingBy(
                instructor -> instructor.getCourse().getId(),
                Collectors.collectingAndThen(
                    Collectors.mapping(
                        instructor -> instructor.getInstructor().getFullName(),
                        Collectors.toList()),
                    names -> new java.util.ArrayList<>(new LinkedHashSet<>(names)))));
  }

  private CourseSummaryResponse toCourseSummaryResponse(
      Course course, List<String> categoryNames, List<String> instructorNames) {
    return CourseSummaryResponse.builder()
        .id(course.getId())
        .title(course.getTitle())
        .slug(course.getSlug())
        .shortDescription(course.getShortDescription())
        .thumbnailUrl(course.getThumbnailUrl())
        .priceBdt(course.getPriceBdt())
        .level(course.getLevel())
        .language(course.getLanguage())
        .publishedAt(course.getPublishedAt())
        .categoryNames(categoryNames.stream().sorted(Comparator.naturalOrder()).toList())
        .instructorNames(instructorNames.stream().sorted(Comparator.naturalOrder()).toList())
        .build();
  }
}
