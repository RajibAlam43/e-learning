package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateCollectionRequest;
import com.gii.api.model.request.admin.SetCollectionCoursesRequest;
import com.gii.api.model.request.admin.UpdateCollectionRequest;
import com.gii.api.model.response.admin.AdminCollectionCourseResponse;
import com.gii.api.model.response.admin.AdminCollectionDetailResponse;
import com.gii.api.model.response.admin.AdminCollectionSummaryResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionCourse;
import com.gii.common.entity.collection.CollectionCourseId;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.user.User;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CourseRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCollectionManagementService {

  private final CollectionRepository collectionRepository;
  private final CollectionCourseRepository collectionCourseRepository;
  private final CourseRepository courseRepository;
  private final CurrentUserService currentUserService;
  private final AssetUrlService assetUrlService;

  @Transactional(readOnly = true)
  public List<AdminCollectionSummaryResponse> list() {
    List<Collection> collections = collectionRepository.findAll();
    List<UUID> ids = collections.stream().map(Collection::getId).toList();
    Map<UUID, Integer> courseCountByCollectionId = new HashMap<>();
    if (!ids.isEmpty()) {
      for (CollectionCourse row : collectionCourseRepository.findByCollection_IdIn(ids)) {
        courseCountByCollectionId.merge(row.getCollection().getId(), 1, Integer::sum);
      }
    }
    return collections.stream()
        .map(
            c ->
                AdminCollectionSummaryResponse.builder()
                    .collectionId(c.getId())
                    .title(c.getTitle())
                    .slug(c.getSlug())
                    .collectionType(c.getType())
                    .thumbnailUrl(assetUrlService.publicUrl(c.getThumbnailObjectKey()))
                    .status(c.getStatus())
                    .priceBdt(c.getPriceBdt())
                    .courseCount(courseCountByCollectionId.getOrDefault(c.getId(), 0))
                    .publishedAt(c.getPublishedAt())
                    .createdAt(c.getCreatedAt())
                    .build())
        .toList();
  }

  public AdminCollectionDetailResponse create(
      CreateCollectionRequest request, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    Collection collection =
        Collection.builder()
            .title(request.title().trim())
            .slug(request.slug().trim())
            .type(request.collectionType())
            .thumbnailObjectKey(
                assetUrlService.normalizeCreateThumbnailKey(
                    request.thumbnailObjectKey(), "collections"))
            .shortDescription(request.shortDescription())
            .description(request.description())
            .priceBdt(request.priceBdt())
            .status(PublishStatus.DRAFT)
            .createdBy(user)
            .build();
    return toDetail(collectionRepository.save(collection));
  }

  @Transactional(readOnly = true)
  public AdminCollectionDetailResponse get(UUID collectionId) {
    Collection collection =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));
    return toDetail(collection);
  }

  public AdminCollectionDetailResponse update(UUID collectionId, UpdateCollectionRequest request) {
    Collection collection =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));
    if (request.title() != null) {
      collection.setTitle(request.title().trim());
    }
    if (request.slug() != null) {
      collection.setSlug(request.slug().trim());
    }
    if (request.collectionType() != null) {
      collection.setType(request.collectionType());
    }
    if (request.thumbnailObjectKey() != null) {
      collection.setThumbnailObjectKey(
          assetUrlService.normalizeThumbnailKey(
              request.thumbnailObjectKey(), "collections", collection.getId()));
    }
    if (request.shortDescription() != null) {
      collection.setShortDescription(request.shortDescription());
    }
    if (request.description() != null) {
      collection.setDescription(request.description());
    }
    if (request.priceBdt() != null) {
      collection.setPriceBdt(request.priceBdt());
    }
    return toDetail(collectionRepository.save(collection));
  }

  public void publish(UUID collectionId) {
    Collection collection =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));
    if (collectionCourseRepository.findByCollection_IdOrderByPositionAsc(collectionId).isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Collection must have at least one course before publishing");
    }
    collection.setStatus(PublishStatus.PUBLISHED);
    collection.setPublishedAt(Instant.now());
    collectionRepository.save(collection);
  }

  public void unpublish(UUID collectionId) {
    Collection collection =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));
    collection.setStatus(PublishStatus.DRAFT);
    collectionRepository.save(collection);
  }

  public AdminCollectionDetailResponse setCourses(UUID collectionId, SetCollectionCoursesRequest request) {
    Collection collection =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));

    HashSet<Integer> seenPositions = new HashSet<>();
    HashSet<UUID> seenCourses = new HashSet<>();
    for (SetCollectionCoursesRequest.Item item : request.items()) {
      if (item.position() == null || item.position() <= 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Position must be positive");
      }
      if (!seenPositions.add(item.position())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate course positions");
      }
      if (!seenCourses.add(item.courseId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate courses in collection");
      }
    }

    List<Course> courses = courseRepository.findAllById(request.items().stream().map(SetCollectionCoursesRequest.Item::courseId).toList());
    Map<UUID, Course> courseById = new HashMap<>();
    for (Course course : courses) {
      courseById.put(course.getId(), course);
    }
    if (courseById.size() != request.items().size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more courses not found");
    }

    collectionCourseRepository.deleteAll(collectionCourseRepository.findByCollection_IdOrderByPositionAsc(collectionId));

    List<CollectionCourse> newRows =
        request.items().stream()
            .map(
                item ->
                    CollectionCourse.builder()
                        .id(
                            CollectionCourseId.builder()
                                .collectionId(collectionId)
                                .courseId(item.courseId())
                                .build())
                        .collection(collection)
                        .course(courseById.get(item.courseId()))
                        .position(item.position())
                        .isMandatory(Boolean.TRUE.equals(item.isMandatory()))
                        .build())
            .toList();
    collectionCourseRepository.saveAll(newRows);
    return toDetail(collection);
  }

  private AdminCollectionDetailResponse toDetail(Collection collection) {
    List<AdminCollectionCourseResponse> courses =
        collectionCourseRepository.findByCollection_IdOrderByPositionAscWithCourse(collection.getId()).stream()
            .map(
                row ->
                    AdminCollectionCourseResponse.builder()
                        .courseId(row.getCourse().getId())
                        .courseTitle(row.getCourse().getTitle())
                        .courseSlug(row.getCourse().getSlug())
                        .position(row.getPosition())
                        .isMandatory(row.getIsMandatory())
                        .build())
            .toList();
    return AdminCollectionDetailResponse.builder()
        .collectionId(collection.getId())
        .title(collection.getTitle())
        .slug(collection.getSlug())
        .collectionType(collection.getType())
        .thumbnailObjectKey(collection.getThumbnailObjectKey())
        .thumbnailUrl(assetUrlService.publicUrl(collection.getThumbnailObjectKey()))
        .shortDescription(collection.getShortDescription())
        .description(collection.getDescription())
        .priceBdt(collection.getPriceBdt())
        .status(collection.getStatus())
        .publishedAt(collection.getPublishedAt())
        .createdAt(collection.getCreatedAt())
        .updatedAt(collection.getUpdatedAt())
        .courses(courses)
        .build();
  }
}
