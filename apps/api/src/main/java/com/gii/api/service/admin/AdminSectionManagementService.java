package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateSectionRequest;
import com.gii.api.model.request.admin.UpdateSectionRequest;
import com.gii.api.model.response.admin.AdminCourseSectionResponse;
import com.gii.api.model.response.admin.AdminLessonSummaryResponse;
import com.gii.api.model.response.admin.AdminLiveClassSectionItemResponse;
import com.gii.api.model.response.admin.AdminQuizSummaryResponse;
import com.gii.api.model.response.admin.AdminSectionItemResponse;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.SectionItem;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import com.gii.common.enums.SectionItemType;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.SectionItemRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.quiz.QuizRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final QuizRepository quizRepository;
  private final SectionItemRepository sectionItemRepository;
  private final LiveClassRepository liveClassRepository;

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
            .titleEn(request.titleEn())
            .slug(request.slug().trim())
            .position(request.position())
            .description(request.description())
            .descriptionEn(request.descriptionEn())
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
    if (request.titleEn() != null) {
      section.setTitleEn(request.titleEn().trim());
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
    if (request.descriptionEn() != null) {
      section.setDescriptionEn(request.descriptionEn());
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

  public void publish(UUID sectionId) {
    CourseSection section =
        sectionRepository
            .findById(sectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
    section.setStatus(PublishStatus.PUBLISHED);
    if (section.getPublishedAt() == null) {
      section.setPublishedAt(Instant.now());
    }
    sectionRepository.save(section);
  }

  public void unpublish(UUID sectionId) {
    CourseSection section =
        sectionRepository
            .findById(sectionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
    section.setStatus(PublishStatus.DRAFT);
    sectionRepository.save(section);
  }

  AdminCourseSectionResponse toResponse(CourseSection section) {
    List<AdminLessonSummaryResponse> lessons =
        lessonRepository.findBySectionIdOrderByPositionAsc(section.getId()).stream()
            .map(this::toLessonSummary)
            .toList();
    List<AdminQuizSummaryResponse> quizzes =
        quizRepository.findBySectionIdOrderByPositionAsc(section.getId()).stream()
            .map(this::toQuizSummary)
            .toList();
    List<LiveClass> liveClasses =
        liveClassRepository.findBySectionIdOrderByStartsAtAsc(section.getId());
    Map<UUID, AdminLessonSummaryResponse> lessonById =
        lessons.stream()
            .collect(Collectors.toMap(AdminLessonSummaryResponse::lessonId, Function.identity()));
    Map<UUID, AdminQuizSummaryResponse> quizById =
        quizzes.stream()
            .collect(Collectors.toMap(AdminQuizSummaryResponse::quizId, Function.identity()));
    Map<UUID, LiveClass> liveClassById =
        liveClasses.stream().collect(Collectors.toMap(LiveClass::getId, Function.identity()));
    List<AdminSectionItemResponse> items =
        sectionItemRepository.findBySectionIdOrderByPositionAsc(section.getId()).stream()
            .map(item -> toSectionItemResponse(item, lessonById, quizById, liveClassById))
            .toList();

    return AdminCourseSectionResponse.builder()
        .sectionId(section.getId())
        .title(section.getTitle())
        .titleEn(section.getTitleEn())
        .slug(section.getSlug())
        .position(section.getPosition())
        .description(section.getDescription())
        .descriptionEn(section.getDescriptionEn())
        .isMandatory(section.getIsMandatory())
        .isFree(section.getIsFree())
        .status(section.getStatus().name())
        .releaseType(section.getReleaseType() != null ? section.getReleaseType().name() : null)
        .releaseAt(section.getReleaseAt())
        .unlockAfterDays(section.getUnlockAfterDays())
        .publishedAt(section.getPublishedAt())
        .createdAt(section.getCreatedAt())
        .updatedAt(section.getUpdatedAt())
        .items(items)
        .build();
  }

  private AdminLessonSummaryResponse toLessonSummary(Lesson lesson) {
    return AdminLessonSummaryResponse.builder()
        .lessonId(lesson.getId())
        .title(lesson.getTitle())
        .titleEn(lesson.getTitleEn())
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

  private AdminQuizSummaryResponse toQuizSummary(Quiz quiz) {
    return AdminQuizSummaryResponse.builder()
        .quizId(quiz.getId())
        .sectionId(quiz.getSection().getId())
        .position(quiz.getPosition())
        .title(quiz.getTitle())
        .status(quiz.getStatus().name())
        .passingScorePct(quiz.getPassingScorePct())
        .maxAttempts(quiz.getMaxAttempts())
        .timeLimitSec(quiz.getTimeLimitSec())
        .createdAt(quiz.getCreatedAt())
        .build();
  }

  private AdminSectionItemResponse toSectionItemResponse(
      SectionItem item,
      Map<UUID, AdminLessonSummaryResponse> lessonById,
      Map<UUID, AdminQuizSummaryResponse> quizById,
      Map<UUID, LiveClass> liveClassById) {
    AdminLessonSummaryResponse lesson = null;
    AdminQuizSummaryResponse quiz = null;
    AdminLiveClassSectionItemResponse liveClass = null;
    if (item.getItemType() == SectionItemType.LESSON) {
      lesson = lessonById.get(item.getItemId());
    } else if (item.getItemType() == SectionItemType.QUIZ) {
      quiz = quizById.get(item.getItemId());
    } else if (item.getItemType() == SectionItemType.LIVE_CLASS) {
      LiveClass value = liveClassById.get(item.getItemId());
      if (value != null) {
        liveClass =
            AdminLiveClassSectionItemResponse.builder()
                .liveClassId(value.getId())
                .title(value.getTitle())
                .titleEn(value.getTitleEn())
                .startsAt(value.getStartsAt())
                .endsAt(value.getEndsAt())
                .provider(value.getProvider())
                .status(value.getStatus())
                .build();
      }
    }
    return AdminSectionItemResponse.builder()
        .itemId(item.getItemId())
        .itemType(item.getItemType().name())
        .position(item.getPosition())
        .lesson(lesson)
        .quiz(quiz)
        .liveClass(liveClass)
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
