package com.gii.api.service.lesson;

import com.gii.api.model.response.lesson.LessonResourceResponse;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.api.service.storage.R2PresignedUrlService.Disposition;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.LessonResource;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.enums.LessonResourcePurpose;
import com.gii.common.repository.course.LessonResourceRepository;
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
public class LessonResourcesService {

  private final LessonAccessService lessonAccessService;
  private final LessonResourceRepository lessonResourceRepository;
  private final LocalizedContentService localizedContentService;
  private final R2PresignedUrlService r2PresignedUrlService;

  public List<LessonResourceResponse> execute(UUID lessonId, Authentication authentication) {
    UUID userId = lessonAccessService.requireCurrentUserId(authentication);
    Lesson lesson = lessonAccessService.requirePublishedLesson(lessonId);
    Enrollment enrollment = lessonAccessService.requireActiveEnrollment(userId, lesson);
    return getResources(lesson, enrollment);
  }

  public List<LessonResourceResponse> execute(
      UUID courseId, UUID lessonId, Authentication authentication) {
    UUID userId = lessonAccessService.requireCurrentUserId(authentication);
    Lesson lesson = lessonAccessService.requirePublishedLesson(lessonId);
    Enrollment enrollment = lessonAccessService.requireActiveEnrollment(userId, courseId, lesson);
    return getResources(lesson, enrollment);
  }

  private List<LessonResourceResponse> getResources(Lesson lesson, Enrollment enrollment) {
    if (!lessonAccessService.isLessonAccessible(lesson, enrollment, Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson is not available yet");
    }

    List<LessonResource> resources =
        lessonResourceRepository.findByLessonIdOrderByPositionAsc(lesson.getId());
    return resources.stream()
        .filter(resource -> resource.getPurpose() == LessonResourcePurpose.SUPPLEMENTARY)
        .map(this::toResponse)
        .toList();
  }

  private LessonResourceResponse toResponse(LessonResource resource) {
    String title = localizedContentService.text(resource.getTitle(), resource.getTitleEn());
    String viewUrl =
        r2PresignedUrlService
            .generateDownloadUrl(
                resource.getFileObjectKey(), title, resource.getMimeType(), Disposition.INLINE)
            .downloadUrl();
    return LessonResourceResponse.builder()
        .resourceId(resource.getId())
        .title(title)
        .resourceType(resource.getResourceType())
        .purpose(resource.getPurpose())
        .mimeType(resource.getMimeType())
        .position(resource.getPosition())
        // Forced-download URLs are minted on demand via the dedicated download-url endpoint.
        .downloadUrl(null)
        .viewUrl(viewUrl)
        .build();
  }
}
