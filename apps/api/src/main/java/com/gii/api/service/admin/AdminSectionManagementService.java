package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateSectionRequest;
import com.gii.api.model.request.admin.UpdateSectionRequest;
import com.gii.api.model.response.admin.AdminCourseSectionResponse;
import com.gii.api.model.response.admin.AdminLessonSummaryResponse;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
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
public class AdminSectionManagementService {

  private final CourseRepository courseRepository;
  private final CourseSectionRepository sectionRepository;
  private final LessonRepository lessonRepository;

  public AdminCourseSectionResponse create(UUID courseId, CreateSectionRequest request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    CourseSection section =
        CourseSection.builder()
            .course(course)
            .title(request.title().trim())
            .slug(request.slug().trim())
            .position(request.position())
            .description(request.description())
            .isMandatory(Boolean.TRUE.equals(request.isMandatory()))
            .isFree(Boolean.TRUE.equals(request.isFree()))
            .releaseType(parseReleaseType(request.releaseType()))
            .releaseAt(request.releaseAt())
            .unlockAfterDays(request.unlockAfterDays())
            .status(PublishStatus.DRAFT)
            .build();
    return toResponse(sectionRepository.save(section));
  }

  public AdminCourseSectionResponse update(UUID sectionId, UpdateSectionRequest request) {
    CourseSection section =
        sectionRepository
            .findById(sectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
    if (request.title() != null) {
      section.setTitle(request.title().trim());
    }
    if (request.slug() != null) {
      section.setSlug(request.slug().trim());
    }
    if (request.position() != null) {
      section.setPosition(request.position());
    }
    if (request.description() != null) {
      section.setDescription(request.description());
    }
    if (request.isMandatory() != null) {
      section.setIsMandatory(request.isMandatory());
    }
    if (request.isFree() != null) {
      section.setIsFree(request.isFree());
    }
    if (request.releaseType() != null) {
      section.setReleaseType(parseReleaseType(request.releaseType()));
    }
    if (request.releaseAt() != null) {
      section.setReleaseAt(request.releaseAt());
    }
    if (request.unlockAfterDays() != null) {
      section.setUnlockAfterDays(request.unlockAfterDays());
    }
    return toResponse(sectionRepository.save(section));
  }

  public void delete(UUID sectionId) {
    CourseSection section =
        sectionRepository
            .findById(sectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
    sectionRepository.delete(section);
  }

  AdminCourseSectionResponse toResponse(CourseSection section) {
    List<AdminLessonSummaryResponse> lessons =
        lessonRepository.findByCourseIdOrderByPositionAsc(section.getCourse().getId()).stream()
            .filter(lesson -> lesson.getSection().getId().equals(section.getId()))
            .map(this::toLessonSummary)
            .toList();

    return AdminCourseSectionResponse.builder()
        .sectionId(section.getId())
        .title(section.getTitle())
        .slug(section.getSlug())
        .position(section.getPosition())
        .description(section.getDescription())
        .isMandatory(section.getIsMandatory())
        .isFree(section.getIsFree())
        .status(section.getStatus().name())
        .releaseType(section.getReleaseType() != null ? section.getReleaseType().name() : null)
        .releaseAt(section.getReleaseAt())
        .unlockAfterDays(section.getUnlockAfterDays())
        .publishedAt(section.getPublishedAt())
        .createdAt(section.getCreatedAt())
        .updatedAt(section.getUpdatedAt())
        .lessons(lessons)
        .build();
  }

  private AdminLessonSummaryResponse toLessonSummary(Lesson lesson) {
    return AdminLessonSummaryResponse.builder()
        .lessonId(lesson.getId())
        .title(lesson.getTitle())
        .slug(lesson.getSlug())
        .position(lesson.getPosition())
        .lessonType(lesson.getLessonType().name())
        .status(lesson.getStatus().name())
        .isMandatory(lesson.getIsMandatory())
        .isFree(lesson.getIsFree())
        .durationSeconds(lesson.getDurationSeconds())
        .createdAt(lesson.getCreatedAt())
        .build();
  }

  private ReleaseType parseReleaseType(String value) {
    if (value == null || value.isBlank()) {
      return ReleaseType.IMMEDIATE;
    }
    try {
      return ReleaseType.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid releaseType");
    }
  }
}
