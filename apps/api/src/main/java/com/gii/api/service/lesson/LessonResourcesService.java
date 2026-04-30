package com.gii.api.service.lesson;

import com.gii.api.model.response.lesson.LessonResourceResponse;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.LessonResource;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.user.User;
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

  public List<LessonResourceResponse> execute(UUID lessonId, Authentication authentication) {
    User user = lessonAccessService.requireCurrentUser(authentication);
    Lesson lesson = lessonAccessService.requirePublishedLesson(lessonId);
    Enrollment enrollment = lessonAccessService.requireActiveEnrollment(user, lesson);
    if (!lessonAccessService.isLessonAccessible(lesson, enrollment, Instant.now())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lesson is not available yet");
    }

    List<LessonResource> resources =
        lessonResourceRepository.findByLessonIdOrderByPositionAsc(lessonId);
    return resources.stream()
        .map(
            resource ->
                LessonResourceResponse.builder()
                    .resourceId(resource.getId())
                    .title(resource.getTitle())
                    .resourceType(resource.getResourceType())
                    .mimeType(resource.getMimeType())
                    .position(resource.getPosition())
                    // Download URL should be requested through signed endpoint.
                    .downloadUrl(null)
                    .build())
        .toList();
  }
}
