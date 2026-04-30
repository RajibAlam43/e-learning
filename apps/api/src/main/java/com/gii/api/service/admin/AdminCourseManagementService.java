package com.gii.api.service.admin;

import com.gii.api.model.request.admin.CreateCourseRequest;
import com.gii.api.model.request.admin.ReorderCourseStructureRequest;
import com.gii.api.model.request.admin.UpdateCourseRequest;
import com.gii.api.model.response.admin.AdminCourseDetailResponse;
import com.gii.api.model.response.admin.AdminCourseSectionResponse;
import com.gii.api.model.response.admin.AdminCourseSummaryResponse;
import com.gii.api.model.response.admin.AdminInstructorSummaryResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
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
@Transactional
public class AdminCourseManagementService {

  private final CourseRepository courseRepository;
  private final CourseSectionRepository sectionRepository;
  private final LessonRepository lessonRepository;
  private final CourseInstructorRepository instructorRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CurrentUserService currentUserService;
  private final AdminSectionManagementService sectionManagementService;

  @Transactional(readOnly = true)
  public List<AdminCourseSummaryResponse> list() {
    return courseRepository.findAll().stream()
        .map(
            course -> {
              String instructorName =
                  instructorRepository.findByCourseId(course.getId()).stream()
                      .findFirst()
                      .map(ci -> ci.getInstructor().getFullName())
                      .orElse(null);
              return AdminCourseSummaryResponse.builder()
                  .courseId(course.getId())
                  .title(course.getTitle())
                  .slug(course.getSlug())
                  .status(course.getStatus())
                  .priceBdt(course.getPriceBdt())
                  .isFree(course.getIsFree())
                  .instructorName(instructorName)
                  .totalEnrolled(
                      (int)
                          enrollmentRepository.countByCourseIdAndStatus(
                              course.getId(), EnrollmentStatus.ACTIVE))
                  .publishedAt(course.getPublishedAt())
                  .createdAt(course.getCreatedAt())
                  .build();
            })
        .toList();
  }

  public AdminCourseDetailResponse create(
      CreateCourseRequest request, Authentication authentication) {
    User user = currentUserService.getCurrentUser(authentication);
    Course course =
        Course.builder()
            .title(request.title().trim())
            .slug(request.slug().trim())
            .thumbnailUrl(request.thumbnailUrl())
            .shortDescription(request.shortDescription())
            .description(request.description())
            .highlights(request.highlights())
            .priceBdt(request.priceBdt())
            .courseOutcomes(request.courseOutcomes())
            .requirements(request.requirements())
            .prerequisites(toList(request.prerequisites()))
            .level(request.level())
            .language(request.language())
            .studyMode(request.studyMode())
            .status(PublishStatus.DRAFT)
            .isFree(Boolean.TRUE.equals(request.isFree()))
            .estimatedDurationMinutes(request.estimatedDurationMinutes())
            .targetAudience(request.targetAudience())
            .liveSessionCount(0)
            .quizCount(0)
            .recordedHoursCount(0)
            .createdBy(user)
            .build();
    return getResponse(courseRepository.save(course));
  }

  @Transactional(readOnly = true)
  public AdminCourseDetailResponse get(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    return getResponse(course);
  }

  public AdminCourseDetailResponse update(UUID courseId, UpdateCourseRequest request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    if (request.title() != null) {
      course.setTitle(request.title().trim());
    }
    if (request.slug() != null) {
      course.setSlug(request.slug().trim());
    }
    if (request.thumbnailUrl() != null) {
      course.setThumbnailUrl(request.thumbnailUrl());
    }
    if (request.shortDescription() != null) {
      course.setShortDescription(request.shortDescription());
    }
    if (request.description() != null) {
      course.setDescription(request.description());
    }
    if (request.highlights() != null) {
      course.setHighlights(request.highlights());
    }
    if (request.priceBdt() != null) {
      course.setPriceBdt(request.priceBdt());
    }
    if (request.courseOutcomes() != null) {
      course.setCourseOutcomes(request.courseOutcomes());
    }
    if (request.requirements() != null) {
      course.setRequirements(request.requirements());
    }
    if (request.prerequisites() != null) {
      course.setPrerequisites(toList(request.prerequisites()));
    }
    if (request.level() != null) {
      course.setLevel(CourseLevel.valueOf(request.level().toUpperCase()));
    }
    if (request.language() != null) {
      course.setLanguage(CourseLanguage.valueOf(request.language().toUpperCase()));
    }
    if (request.studyMode() != null) {
      course.setStudyMode(StudyMode.valueOf(request.studyMode().toUpperCase()));
    }
    if (request.isFree() != null) {
      course.setIsFree(request.isFree());
    }
    if (request.estimatedDurationMinutes() != null) {
      course.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
    }
    if (request.targetAudience() != null) {
      course.setTargetAudience(request.targetAudience());
    }
    return getResponse(courseRepository.save(course));
  }

  public void publish(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    course.setStatus(PublishStatus.PUBLISHED);
    course.setPublishedAt(Instant.now());
    courseRepository.save(course);
  }

  public void unpublish(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    course.setStatus(PublishStatus.DRAFT);
    courseRepository.save(course);
  }

  public void reorder(UUID courseId, ReorderCourseStructureRequest request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    for (var secReq : request.sections()) {
      CourseSection sec =
          sectionRepository
              .findById(secReq.sectionId())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
      if (!sec.getCourse().getId().equals(course.getId())) {
        continue;
      }
      sec.setPosition(secReq.newPosition());
      sectionRepository.save(sec);
      if (secReq.lessons() != null) {
        for (var lessonReq : secReq.lessons()) {
          lessonRepository
              .findById(lessonReq.lessonId())
              .ifPresent(
                  lesson -> {
                    if (lesson.getSection().getId().equals(sec.getId())) {
                      lesson.setPosition(lessonReq.newPosition());
                      lessonRepository.save(lesson);
                    }
                  });
        }
      }
    }
  }

  private AdminCourseDetailResponse getResponse(Course course) {
    List<AdminCourseSectionResponse> sections =
        sectionRepository.findByCourseIdOrderByPositionAsc(course.getId()).stream()
            .map(sectionManagementService::toResponse)
            .toList();

    List<AdminInstructorSummaryResponse> instructors =
        instructorRepository.findByCourseId(course.getId()).stream()
            .map(CourseInstructor::getInstructor)
            .map(
                i ->
                    AdminInstructorSummaryResponse.builder()
                        .userId(i.getId())
                        .fullName(i.getFullName())
                        .email(i.getEmail())
                        .displayName(i.getFullName())
                        .headline(null)
                        .isPublic(true)
                        .assignedCoursesCount(0)
                        .createdAt(i.getCreatedAt())
                        .build())
            .toList();

    return AdminCourseDetailResponse.builder()
        .courseId(course.getId())
        .title(course.getTitle())
        .slug(course.getSlug())
        .thumbnailUrl(course.getThumbnailUrl())
        .shortDescription(course.getShortDescription())
        .description(course.getDescription())
        .highlights(course.getHighlights())
        .priceBdt(course.getPriceBdt())
        .courseOutcomes(course.getCourseOutcomes())
        .requirements(course.getRequirements())
        .level(course.getLevel())
        .language(course.getLanguage())
        .studyMode(course.getStudyMode())
        .status(course.getStatus())
        .isFree(course.getIsFree())
        .liveSessionCount(course.getLiveSessionCount())
        .quizCount(course.getQuizCount())
        .recordedHoursCount(course.getRecordedHoursCount())
        .estimatedDurationMinutes(course.getEstimatedDurationMinutes())
        .targetAudience(course.getTargetAudience())
        .prerequisites(
            course.getPrerequisites() != null ? String.join(", ", course.getPrerequisites()) : null)
        .createdBy(course.getCreatedBy() != null ? course.getCreatedBy().getId() : null)
        .publishedAt(course.getPublishedAt())
        .createdAt(course.getCreatedAt())
        .updatedAt(course.getUpdatedAt())
        .sections(sections)
        .instructors(instructors)
        .build();
  }

  private List<String> toList(String commaSeparated) {
    if (commaSeparated == null || commaSeparated.isBlank()) {
      return List.of();
    }
    return List.of(commaSeparated.split(",")).stream()
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();
  }
}
