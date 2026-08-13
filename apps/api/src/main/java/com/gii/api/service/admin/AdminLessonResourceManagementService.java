package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateLessonResourceRequest;
import com.gii.api.model.request.admin.CreateLessonResourceUploadRequest;
import com.gii.api.model.request.admin.UpdateLessonResourceRequest;
import com.gii.api.model.response.admin.AdminLessonResourceResponse;
import com.gii.api.model.response.admin.LessonResourceUploadResponse;
import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.LessonResource;
import com.gii.common.enums.LessonResourceType;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.LessonResourceRepository;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminLessonResourceManagementService {

  private static final Pattern FILE_NAME =
      Pattern.compile("(?i)[a-z0-9][a-z0-9._-]*\\.(pdf|jpe?g|png|webp)");
  private static final Map<String, String> CONTENT_TYPES =
      Map.of(
          "pdf", "application/pdf",
          "jpg", "image/jpeg",
          "jpeg", "image/jpeg",
          "png", "image/png",
          "webp", "image/webp");

  private final LessonRepository lessonRepository;
  private final LessonResourceRepository lessonResourceRepository;
  private final R2PresignedUrlService r2PresignedUrlService;

  @Transactional(readOnly = true)
  public LessonResourceUploadResponse createUpload(
      UUID lessonId, CreateLessonResourceUploadRequest request) {
    requireLesson(lessonId);
    String filename = request.filename().trim();
    if (!FILE_NAME.matcher(filename).matches()) {
      throw invalidUpload();
    }
    int extensionStart = filename.lastIndexOf('.');
    String extension = filename.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
    String contentType = CONTENT_TYPES.get(extension);
    if (contentType == null || !contentType.equalsIgnoreCase(request.contentType().trim())) {
      throw invalidUpload();
    }
    String storedFilename =
        filename.substring(0, extensionStart)
            + "-"
            + UUID.randomUUID().toString().substring(0, 8)
            + filename.substring(extensionStart).toLowerCase(Locale.ROOT);
    String objectKey = "lesson-resources/" + lessonId + "/" + storedFilename;
    R2PresignedUrlService.PresignedUpload upload =
        r2PresignedUrlService.generateUploadUrl(objectKey, contentType, request.sizeBytes());
    return LessonResourceUploadResponse.builder()
        .objectKey(objectKey)
        .uploadUrl(upload.uploadUrl())
        .method("PUT")
        .contentType(contentType)
        .sizeBytes(request.sizeBytes())
        .expiresAt(upload.expiresAt())
        .build();
  }

  public AdminLessonResourceResponse create(UUID lessonId, CreateLessonResourceRequest request) {
    final Lesson lesson = requireLesson(lessonId);
    validateObjectKey(lessonId, request.objectKey());
    validateTypeAndMime(request.resourceType(), request.mimeType());
    ensurePositionAvailable(lessonId, request.position(), null);
    LessonResource resource =
        LessonResource.builder()
            .lesson(lesson)
            .title(request.title().trim())
            .titleEn(trimToNull(request.titleEn()))
            .resourceType(request.resourceType())
            .mimeType(request.mimeType().trim().toLowerCase(Locale.ROOT))
            .fileObjectKey(request.objectKey().trim())
            .fileUrl(request.objectKey().trim())
            .position(request.position())
            .build();
    return toResponse(lessonResourceRepository.save(resource));
  }

  public AdminLessonResourceResponse update(UUID resourceId, UpdateLessonResourceRequest request) {
    LessonResource resource = findResource(resourceId);
    if (request.title() != null) {
      if (request.title().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource title is required");
      }
      resource.setTitle(request.title().trim());
    }
    if (request.titleEn() != null) {
      resource.setTitleEn(trimToNull(request.titleEn()));
    }
    LessonResourceType type =
        request.resourceType() != null ? request.resourceType() : resource.getResourceType();
    String mimeType = request.mimeType() != null ? request.mimeType() : resource.getMimeType();
    validateTypeAndMime(type, mimeType);
    resource.setResourceType(type);
    resource.setMimeType(mimeType.trim().toLowerCase(Locale.ROOT));
    if (request.objectKey() != null) {
      validateObjectKey(resource.getLesson().getId(), request.objectKey());
      resource.setFileObjectKey(request.objectKey().trim());
      resource.setFileUrl(request.objectKey().trim());
    }
    if (request.position() != null) {
      ensurePositionAvailable(resource.getLesson().getId(), request.position(), resourceId);
      resource.setPosition(request.position());
    }
    return toResponse(lessonResourceRepository.save(resource));
  }

  public void delete(UUID resourceId) {
    lessonResourceRepository.delete(findResource(resourceId));
  }

  private Lesson requireLesson(UUID lessonId) {
    return lessonRepository
        .findById(lessonId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
  }

  private LessonResource findResource(UUID resourceId) {
    return lessonResourceRepository
        .findById(resourceId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson resource not found"));
  }

  private void validateObjectKey(UUID lessonId, String objectKey) {
    String expectedPrefix = "lesson-resources/" + lessonId + "/";
    if (objectKey == null
        || !objectKey.trim().startsWith(expectedPrefix)
        || !objectKey
            .trim()
            .substring(expectedPrefix.length())
            .matches(".+-[0-9a-fA-F]{8}\\.[^.]+")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid lesson resource object key");
    }
  }

  private void validateTypeAndMime(LessonResourceType type, String mimeType) {
    if (type == null || mimeType == null || mimeType.isBlank()) {
      throw invalidUpload();
    }
    String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
    boolean valid =
        type == LessonResourceType.PDF
            ? normalized.equals("application/pdf")
            : normalized.equals("image/jpeg")
                || normalized.equals("image/png")
                || normalized.equals("image/webp");
    if (!valid) {
      throw invalidUpload();
    }
  }

  private void ensurePositionAvailable(UUID lessonId, Integer position, UUID currentResourceId) {
    lessonResourceRepository.findByLessonIdOrderByPositionAsc(lessonId).stream()
        .filter(resource -> resource.getPosition().equals(position))
        .filter(resource -> !resource.getId().equals(currentResourceId))
        .findFirst()
        .ifPresent(
            resource -> {
              throw new ResponseStatusException(
                  HttpStatus.BAD_REQUEST, "Resource position is already used");
            });
  }

  private AdminLessonResourceResponse toResponse(LessonResource resource) {
    return AdminLessonResourceResponse.builder()
        .resourceId(resource.getId())
        .lessonId(resource.getLesson().getId())
        .title(resource.getTitle())
        .titleEn(resource.getTitleEn())
        .resourceType(resource.getResourceType())
        .mimeType(resource.getMimeType())
        .fileUrl(resource.getFileUrl())
        .objectKey(resource.getFileObjectKey())
        .position(resource.getPosition())
        .createdAt(resource.getCreatedAt())
        .updatedAt(resource.getUpdatedAt())
        .build();
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private ResponseStatusException invalidUpload() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Invalid lesson resource type or content type");
  }
}
