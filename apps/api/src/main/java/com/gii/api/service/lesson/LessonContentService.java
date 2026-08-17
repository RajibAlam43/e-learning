package com.gii.api.service.lesson;

import com.gii.api.model.response.lesson.LessonContentResponse;
import com.gii.api.model.response.lesson.LessonProgressResponse;
import com.gii.api.model.response.lesson.LessonResourceResponse;
import com.gii.api.model.response.lesson.MediaPlaybackResponse;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.enums.LessonResourcePurpose;
import com.gii.common.enums.MediaStatus;
import com.gii.common.repository.course.LessonResourceRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonContentService {

  private final LessonAccessService lessonAccessService;
  private final LessonProgressRepository lessonProgressRepository;
  private final MediaAssetRepository mediaAssetRepository;
  private final LessonResourceRepository lessonResourceRepository;
  private final AssetUrlService assetUrlService;
  private final LocalizedContentService localizedContentService;

  public LessonContentResponse execute(UUID lessonId, Authentication authentication) {
    UUID userId = lessonAccessService.requireCurrentUserId(authentication);
    Lesson lesson = lessonAccessService.requirePublishedLesson(lessonId);
    Enrollment enrollment = lessonAccessService.requireActiveEnrollment(userId, lesson);

    Instant now = Instant.now();
    boolean accessible = lessonAccessService.isLessonAccessible(lesson, enrollment, now);
    if (!accessible) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson is not available yet");
    }

    LessonProgress progress =
        lessonProgressRepository
            .findById(
                com.gii.common.entity.enrollment.LessonProgressId.builder()
                    .userId(userId)
                    .lessonId(lessonId)
                    .build())
            .orElse(null);

    String courseThumbnailUrl =
        assetUrlService.publicUrl(lesson.getCourse().getThumbnailObjectKey());
    MediaPlaybackResponse media =
        toLessonPlayback(
            mediaAssetRepository.findByLessonId(lessonId).orElse(null), courseThumbnailUrl);
    List<LessonResourceResponse> allResources =
        lessonResourceRepository.findByLessonIdOrderByPositionAsc(lessonId).stream()
            .map(
                resource ->
                    LessonResourceResponse.builder()
                        .resourceId(resource.getId())
                        .title(
                            localizedContentService.text(
                                resource.getTitle(), resource.getTitleEn()))
                        .resourceType(resource.getResourceType())
                        .purpose(resource.getPurpose())
                        .mimeType(resource.getMimeType())
                        .position(resource.getPosition())
                        .downloadUrl(null)
                        .build())
            .toList();
    LessonResourceResponse primaryResource =
        allResources.stream()
            .filter(resource -> resource.purpose() == LessonResourcePurpose.PRIMARY_CONTENT)
            .findFirst()
            .orElse(null);
    List<LessonResourceResponse> resources =
        allResources.stream()
            .filter(resource -> resource.purpose() == LessonResourcePurpose.SUPPLEMENTARY)
            .toList();

    return LessonContentResponse.builder()
        .lessonId(lesson.getId())
        .title(localizedContentService.text(lesson.getTitle(), lesson.getTitleEn()))
        .slug(lesson.getSlug())
        .position(lesson.getPosition())
        .lessonType(lesson.getLessonType())
        .description(null)
        .durationSeconds(lesson.getDurationSeconds())
        .transcriptUrl(lesson.getTranscriptUrl())
        .isFree(lesson.getIsFree())
        .isMandatory(lesson.getIsMandatory())
        .isAccessible(true)
        .accessReason("ENROLLED")
        .releaseAt(lesson.getReleaseAt())
        .unlockAfterDays(lesson.getUnlockAfterDays())
        .userProgress(toProgress(progress))
        .mediaPlayback(media)
        .primaryResource(primaryResource)
        .resources(resources)
        .courseId(lesson.getCourse().getId())
        .sectionId(lesson.getSection().getId())
        .courseName(
            localizedContentService.text(
                lesson.getCourse().getTitle(), lesson.getCourse().getTitleEn()))
        .build();
  }

  private LessonProgressResponse toProgress(LessonProgress progress) {
    if (progress == null) {
      return LessonProgressResponse.builder()
          .completed(false)
          .completedAt(null)
          .lastPositionSec(0)
          .lastUpdatedAt(null)
          .build();
    }
    return LessonProgressResponse.builder()
        .completed(progress.getCompletedAt() != null)
        .completedAt(progress.getCompletedAt())
        .lastPositionSec(progress.getLastPositionSec())
        .lastUpdatedAt(progress.getUpdatedAt())
        .build();
  }

  private MediaPlaybackResponse toLessonPlayback(MediaAsset mediaAsset, String courseThumbnailUrl) {
    if (mediaAsset == null || mediaAsset.getStatus() != MediaStatus.READY) {
      return null;
    }

    return MediaPlaybackResponse.builder()
        .mediaAssetId(mediaAsset.getId())
        .assetType(mediaAsset.getAssetType())
        .provider(mediaAsset.getProvider())
        .title(localizedContentService.text(mediaAsset.getTitle(), mediaAsset.getTitleEn()))
        .durationSec(mediaAsset.getDurationSec())
        .maxResolution(mediaAsset.getMaxResolution())
        .youtubeVideoId(
            mediaAsset.getProvider() == com.gii.common.enums.MediaProvider.YOUTUBE
                ? mediaAsset.getProviderAssetId()
                : null)
        .muxPlaybackId(
            mediaAsset.getProvider() == com.gii.common.enums.MediaProvider.MUX
                ? mediaAsset.getPlaybackId()
                : null)
        .bunnyAssetId(
            mediaAsset.getProvider() == com.gii.common.enums.MediaProvider.BUNNY
                ? mediaAsset.getProviderAssetId()
                : null)
        .fileUrl(mediaAsset.getFileUrl())
        .preferredPlaybackMode(mediaAsset.getPreferredPlaybackMode())
        .requiresSignedUrl(mediaAsset.getProvider() != com.gii.common.enums.MediaProvider.YOUTUBE)
        .subtitlesUrl(null)
        .thumbnailUrl(
            assetUrlService.publicUrl(mediaAsset.getThumbnailObjectKey()) != null
                ? assetUrlService.publicUrl(mediaAsset.getThumbnailObjectKey())
                : courseThumbnailUrl)
        .build();
  }
}
