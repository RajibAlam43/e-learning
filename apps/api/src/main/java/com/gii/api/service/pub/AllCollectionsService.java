package com.gii.api.service.pub;

import com.gii.api.model.response.CollectionSummaryResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
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
public class AllCollectionsService {

  private static final int MAX_PAGE_SIZE = 20;
  private static final String DEFAULT_SORT_FIELD = "publishedAt";
  private static final List<String> ALLOWED_SORT_FIELDS =
      List.of("publishedAt", "priceBdt", "title");

  private final CollectionRepository collectionRepository;
  private final CollectionCourseRepository collectionCourseRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final AssetUrlService assetUrlService;
  private final LocalizedContentService localizedContentService;

  public PageResponse<CollectionSummaryResponse> execute(CollectionType collectionType, Pageable pageable) {
    Pageable safePageable = createSafePageable(pageable);

    Specification<Collection> spec =
        Specification.where(CollectionSpecifications.hasStatus(PublishStatus.PUBLISHED))
            .and(CollectionSpecifications.hasType(collectionType));

    Page<Collection> collectionPage = collectionRepository.findAll(spec, safePageable);
    List<Collection> collections = collectionPage.getContent();
    List<UUID> collectionIds = collections.stream().map(Collection::getId).toList();

    List<CollectionCourse> collectionCourses =
        collectionIds.isEmpty()
            ? List.of()
            : collectionCourseRepository.findByCollection_IdInWithCourseStatus(
                collectionIds, PublishStatus.PUBLISHED);

    Map<UUID, List<UUID>> courseIdsByCollectionId = new HashMap<>();
    for (CollectionCourse collectionCourse : collectionCourses) {
      courseIdsByCollectionId
          .computeIfAbsent(collectionCourse.getCollection().getId(), ignored -> new java.util.ArrayList<>())
          .add(collectionCourse.getCourse().getId());
    }

    List<UUID> allCourseIds =
        collectionCourses.stream().map(collectionCourse -> collectionCourse.getCourse().getId()).distinct().toList();
    Map<UUID, List<String>> instructorNamesByCourseId = getInstructorNamesByCourseId(allCourseIds);

    List<CollectionSummaryResponse> content =
        collections.stream()
            .map(
                collection -> {
                  List<UUID> courseIds = courseIdsByCollectionId.getOrDefault(collection.getId(), List.of());
                  LinkedHashSet<String> instructorNames = new LinkedHashSet<>();
                  for (UUID courseId : courseIds) {
                    instructorNames.addAll(instructorNamesByCourseId.getOrDefault(courseId, List.of()));
                  }
                  return CollectionSummaryResponse.builder()
                      .id(collection.getId())
                      .title(
                          localizedContentService.text(
                              collection.getTitle(), collection.getTitleEn()))
                      .slug(collection.getSlug())
                      .collectionType(collection.getType())
                      .shortDescription(
                          localizedContentService.text(
                              collection.getShortDescription(),
                              collection.getShortDescriptionEn()))
                      .thumbnailUrl(assetUrlService.publicUrl(collection.getThumbnailObjectKey()))
                      .priceBdt(collection.getPriceBdt())
                      .publishedAt(collection.getPublishedAt())
                      .courseCount(courseIds.size())
                      .instructorNames(instructorNames.stream().sorted().toList())
                      .build();
                })
            .toList();

    return PageResponse.<CollectionSummaryResponse>builder()
        .content(content)
        .page(collectionPage.getNumber())
        .size(collectionPage.getSize())
        .totalElements(collectionPage.getTotalElements())
        .totalPages(collectionPage.getTotalPages())
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
        requestedSort.stream().filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty())).toList();

    if (safeOrders.isEmpty()) {
      return defaultSort;
    }

    if (safeOrders.stream().noneMatch(order -> order.getProperty().equals("id"))) {
      safeOrders = new java.util.ArrayList<>(safeOrders);
      safeOrders.add(Sort.Order.desc("id"));
    }
    return Sort.by(safeOrders);
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
                    Collectors.mapping(instructor -> instructor.getInstructor().getFullName(), Collectors.toList()),
                    names -> new java.util.ArrayList<>(new LinkedHashSet<>(names)))));
  }
}
