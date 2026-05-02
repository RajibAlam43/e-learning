package com.gii.api.service.admin;

import com.gii.api.model.request.lesson.CreateLessonRequest;
import com.gii.api.model.request.lesson.UpdateLessonRequest;
import com.gii.api.model.response.admin.AdminLessonDetailResponse;
import com.gii.api.model.response.admin.AdminLessonResourceResponse;
import com.gii.api.model.response.admin.AdminMediaAssetResponse;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.course.SectionItem;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import com.gii.common.enums.SectionItemType;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.LessonResourceRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import com.gii.common.repository.course.SectionItemRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminLessonManagementService {

  private final CourseSectionRepository sectionRepository;
  private final LessonRepository lessonRepository;
  private final MediaAssetRepository mediaAssetRepository;
  private final LessonResourceRepository resourceRepository;
  private final SectionItemRepository sectionItemRepository;

  public AdminLessonDetailResponse create(UUID sectionId, CreateLessonRequest request) {
    CourseSection section =
        sectionRepository
            .findById(sectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

    ensurePositionAvailable(section.getId(), request.position(), null);

    Lesson lesson =
        Lesson.builder()
            .course(section.getCourse())
            .section(section)
            .title(request.title().trim())
            .slug(request.slug().trim())
            .position(request.position())
            .lessonType(parseLessonType(request.lessonType()))
            .isMandatory(Boolean.TRUE.equals(request.isMandatory()))
            .isFree(Boolean.TRUE.equals(request.isFree()))
            .durationSeconds(request.durationSeconds())
            .thumbnailUrl(request.thumbnailUrl())
            .transcriptUrl(request.transcriptUrl())
            .releaseType(
                request.releaseType() == null
                    ? ReleaseType.IMMEDIATE
                    : ReleaseType.valueOf(request.releaseType()))
            .releaseAt(request.releaseAt())
            .unlockAfterDays(request.unlockAfterDays())
            .status(PublishStatus.DRAFT)
            .build();
    Lesson saved = lessonRepository.save(lesson);
    sectionItemRepository.save(
        SectionItem.builder()
            .section(section)
            .itemType(SectionItemType.LESSON)
            .itemId(saved.getId())
            .position(saved.getPosition())
            .build());
    return toDetail(saved);
  }

  public AdminLessonDetailResponse update(UUID lessonId, UpdateLessonRequest request) {
    Lesson lesson =
        lessonRepository
            .findById(lessonId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    if (request.title() != null) {
      lesson.setTitle(request.title().trim());
    }
    if (request.slug() != null) {
      lesson.setSlug(request.slug().trim());
    }
    if (request.position() != null) {
      ensurePositionAvailable(lesson.getSection().getId(), request.position(), lesson.getId());
      lesson.setPosition(request.position());
    }
    if (request.lessonType() != null) {
      lesson.setLessonType(parseLessonType(request.lessonType()));
    }
    if (request.isMandatory() != null) {
      lesson.setIsMandatory(request.isMandatory());
    }
    if (request.isFree() != null) {
      lesson.setIsFree(request.isFree());
    }
    if (request.durationSeconds() != null) {
      lesson.setDurationSeconds(request.durationSeconds());
    }
    if (request.thumbnailUrl() != null) {
      lesson.setThumbnailUrl(request.thumbnailUrl());
    }
    if (request.transcriptUrl() != null) {
      lesson.setTranscriptUrl(request.transcriptUrl());
    }
    if (request.releaseType() != null) {
      lesson.setReleaseType(ReleaseType.valueOf(request.releaseType().toUpperCase()));
    }
    if (request.releaseAt() != null) {
      lesson.setReleaseAt(request.releaseAt());
    }
    if (request.unlockAfterDays() != null) {
      lesson.setUnlockAfterDays(request.unlockAfterDays());
    }
    Lesson saved = lessonRepository.save(lesson);
    SectionItem sectionItem =
        sectionItemRepository
            .findByItemTypeAndItemId(SectionItemType.LESSON, saved.getId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Section item missing"));
    sectionItem.setPosition(saved.getPosition());
    sectionItemRepository.save(sectionItem);
    return toDetail(saved);
  }

  public void delete(UUID lessonId) {
    Lesson lesson =
        lessonRepository
            .findById(lessonId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    sectionItemRepository.deleteByItemTypeAndItemId(SectionItemType.LESSON, lesson.getId());
    lessonRepository.delete(lesson);
  }

  private void ensurePositionAvailable(
      UUID sectionId, Integer requestedPosition, UUID currentLessonId) {
    if (requestedPosition == null || requestedPosition <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "position must be positive");
    }
    sectionItemRepository
        .findBySectionIdAndPosition(sectionId, requestedPosition)
        .ifPresent(
            item -> {
              boolean sameLesson =
                  item.getItemType() == SectionItemType.LESSON
                      && currentLessonId != null
                      && currentLessonId.equals(item.getItemId());
              if (!sameLesson) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Position is already used in this section");
              }
            });
  }

  AdminLessonDetailResponse toDetail(Lesson lesson) {
    MediaAsset mediaAsset = mediaAssetRepository.findByLessonId(lesson.getId()).orElse(null);
    AdminMediaAssetResponse media =
        mediaAsset == null
            ? null
            : AdminMediaAssetResponse.builder()
                .mediaAssetId(mediaAsset.getId())
                .lessonId(lesson.getId())
                .provider(mediaAsset.getProvider().name())
                .assetType(mediaAsset.getAssetType().name())
                .providerAssetId(mediaAsset.getProviderAssetId())
                .providerLibraryId(mediaAsset.getProviderLibraryId())
                .playbackId(mediaAsset.getPlaybackId())
                .playbackPolicy(
                    mediaAsset.getPlaybackPolicy() != null
                        ? mediaAsset.getPlaybackPolicy().name()
                        : null)
                .fileUrl(mediaAsset.getFileUrl())
                .title(mediaAsset.getTitle())
                .maxResolution(mediaAsset.getMaxResolution())
                .durationSec(mediaAsset.getDurationSec())
                .status(mediaAsset.getStatus().name())
                .preferredPlaybackMode(
                    mediaAsset.getPreferredPlaybackMode() != null
                        ? mediaAsset.getPreferredPlaybackMode().name()
                        : null)
                .createdBy(
                    mediaAsset.getCreatedBy() != null ? mediaAsset.getCreatedBy().getId() : null)
                .createdAt(mediaAsset.getCreatedAt())
                .updatedAt(mediaAsset.getUpdatedAt())
                .build();

    List<AdminLessonResourceResponse> resources =
        resourceRepository.findByLessonIdOrderByPositionAsc(lesson.getId()).stream()
            .map(
                r ->
                    AdminLessonResourceResponse.builder()
                        .resourceId(r.getId())
                        .lessonId(lesson.getId())
                        .title(r.getTitle())
                        .resourceType(r.getResourceType())
                        .mimeType(r.getMimeType())
                        .fileUrl(r.getFileUrl())
                        .position(r.getPosition())
                        .createdAt(r.getCreatedAt())
                        .updatedAt(r.getUpdatedAt())
                        .build())
            .toList();

    return AdminLessonDetailResponse.builder()
        .lessonId(lesson.getId())
        .title(lesson.getTitle())
        .slug(lesson.getSlug())
        .position(lesson.getPosition())
        .lessonType(lesson.getLessonType().name())
        .status(lesson.getStatus().name())
        .isMandatory(lesson.getIsMandatory())
        .isFree(lesson.getIsFree())
        .durationSeconds(lesson.getDurationSeconds())
        .thumbnailUrl(lesson.getThumbnailUrl())
        .transcriptUrl(lesson.getTranscriptUrl())
        .releaseType(lesson.getReleaseType() != null ? lesson.getReleaseType().name() : null)
        .releaseAt(lesson.getReleaseAt())
        .unlockAfterDays(lesson.getUnlockAfterDays())
        .publishedAt(null)
        .createdAt(lesson.getCreatedAt())
        .updatedAt(lesson.getUpdatedAt())
        .mediaAsset(media)
        .resources(resources)
        .build();
  }

  private LessonType parseLessonType(String lessonType) {
    try {
      return LessonType.valueOf(lessonType.trim().toUpperCase());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lessonType");
    }
  }
}
