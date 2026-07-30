package com.gii.api.service.pub;

import com.gii.api.model.response.CollectionCourseSummaryResponse;
import com.gii.api.model.response.CollectionDetailsResponse;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionDetailsService {

  private final CollectionRepository collectionRepository;
  private final CollectionCourseRepository collectionCourseRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final AssetUrlService assetUrlService;
  private final LocalizedContentService localizedContentService;

  public CollectionDetailsResponse execute(String slug) {
    Collection collection =
        collectionRepository
            .findBySlugAndStatus(slug, PublishStatus.PUBLISHED)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

    List<CollectionCourse> collectionCourses =
        collectionCourseRepository.findByCollection_IdOrderByPositionAscWithCourseStatus(
            collection.getId(), PublishStatus.PUBLISHED);

    List<UUID> courseIds = collectionCourses.stream().map(cc -> cc.getCourse().getId()).toList();
    List<CourseInstructor> instructors =
        courseIds.isEmpty() ? List.of() : courseInstructorRepository.findByCourseIds(courseIds);

    LinkedHashSet<String> instructorNames =
        instructors.stream()
            .map(instructor -> instructor.getInstructor().getFullName())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    List<CollectionCourseSummaryResponse> courses =
        collectionCourses.stream()
            .map(
                collectionCourse ->
                    CollectionCourseSummaryResponse.builder()
                        .id(collectionCourse.getCourse().getId())
                        .title(
                            localizedContentService.text(
                                collectionCourse.getCourse().getTitle(),
                                collectionCourse.getCourse().getTitleEn()))
                        .slug(collectionCourse.getCourse().getSlug())
                        .thumbnailUrl(
                            assetUrlService.publicUrl(
                                collectionCourse.getCourse().getThumbnailObjectKey()))
                        .position(collectionCourse.getPosition())
                        .isMandatory(collectionCourse.getIsMandatory())
                        .build())
            .toList();

    return CollectionDetailsResponse.builder()
        .id(collection.getId())
        .title(localizedContentService.text(collection.getTitle(), collection.getTitleEn()))
        .slug(collection.getSlug())
        .collectionType(collection.getType())
        .thumbnailUrl(assetUrlService.publicUrl(collection.getThumbnailObjectKey()))
        .shortDescription(
            localizedContentService.text(
                collection.getShortDescription(), collection.getShortDescriptionEn()))
        .description(
            localizedContentService.text(
                collection.getDescription(), collection.getDescriptionEn()))
        .priceBdt(collection.getPriceBdt())
        .publishedAt(collection.getPublishedAt())
        .courseCount(courses.size())
        .instructorNames(instructorNames.stream().sorted().toList())
        .courses(courses)
        .build();
  }
}
