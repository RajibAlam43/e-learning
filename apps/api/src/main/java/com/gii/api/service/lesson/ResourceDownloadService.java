package com.gii.api.service.lesson;

import com.gii.api.model.response.lesson.ResourceDownloadUrlResponse;
import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.LessonResource;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.repository.course.LessonResourceRepository;
import java.time.Instant;
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
public class ResourceDownloadService {

  private final LessonAccessService lessonAccessService;
  private final LessonResourceRepository lessonResourceRepository;
  private final R2PresignedUrlService r2PresignedUrlService;

  public ResourceDownloadUrlResponse execute(UUID resourceId, Authentication authentication) {
    UUID userId = lessonAccessService.requireCurrentUserId(authentication);
    LessonResource resource =
        lessonResourceRepository
            .findById(resourceId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

    Lesson lesson = lessonAccessService.requirePublishedLesson(resource.getLesson().getId());
    Enrollment enrollment = lessonAccessService.requireActiveEnrollment(userId, lesson);
    if (!lessonAccessService.isLessonAccessible(lesson, enrollment, Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson is not available yet");
    }

    R2PresignedUrlService.PresignedDownload signed =
        r2PresignedUrlService.generateDownloadUrl(
            resource.getFileUrl(), resource.getTitle(), resource.getMimeType());

    return ResourceDownloadUrlResponse.builder()
        .downloadUrl(signed.downloadUrl())
        .expiresAt(signed.expiresAt())
        .fileName(resource.getTitle())
        .build();
  }
}
