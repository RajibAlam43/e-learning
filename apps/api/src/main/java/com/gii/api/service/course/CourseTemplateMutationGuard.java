package com.gii.api.service.course;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseTemplateVersion;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CourseTemplateMutationGuard {

  private final CourseRepository courseRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CollectionCourseRepository collectionCourseRepository;
  private final CourseTemplateVersionCloneService cloneService;

  public Course prepareForTemplateUpdate(Course course) {
    CourseTemplateVersion version = course.getTemplateVersion();
    if (isDirectlyMutable(version)) {
      return course;
    }
    if (enrollmentRepository.existsByCourseId(course.getId())) {
      throw immutableCurriculum();
    }
    if (collectionCourseRepository.existsByCourseIdAndCollectionStatus(
        course.getId(), PublishStatus.PUBLISHED)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Course belongs to a published collection; unpublish it before editing curriculum");
    }

    course.setTemplateVersion(cloneService.cloneForEditing(course));
    course.setStatus(PublishStatus.DRAFT);
    course.setPublishedAt(null);
    course.setIsFeatured(false);
    course.setFeaturedPosition(null);
    course.setFeaturedAt(null);
    return courseRepository.save(course);
  }

  public void requireDraft(CourseTemplateVersion version) {
    if (!isDirectlyMutable(version)) {
      throw immutableCurriculum();
    }
  }

  private boolean isDirectlyMutable(CourseTemplateVersion version) {
    return version != null
        && version.getStatus() == PublishStatus.DRAFT
        && courseRepository.countByTemplateVersionId(version.getId()) == 1
        && !enrollmentRepository.existsByTemplateVersionId(version.getId());
  }

  private ResponseStatusException immutableCurriculum() {
    return new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Published, shared, or in-use curriculum is immutable; "
            + "repeat the course to edit a new version");
  }
}
