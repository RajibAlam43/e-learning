package com.gii.api.service.instructor;

import com.gii.api.mapper.CourseAnnouncementMapper;
import com.gii.api.model.request.instructor.CreateCourseAnnouncementRequest;
import com.gii.api.model.response.CourseAnnouncementResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseAnnouncement;
import com.gii.common.entity.user.User;
import com.gii.common.repository.course.CourseAnnouncementRepository;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.course.CourseRepository;
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
public class CourseAnnouncementCreationService {

  private final CurrentUserService currentUserService;
  private final CourseRepository courseRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final CourseAnnouncementRepository courseAnnouncementRepository;
  private final CourseAnnouncementMapper courseAnnouncementMapper;

  public CourseAnnouncementResponse execute(
      UUID courseId, CreateCourseAnnouncementRequest request, Authentication authentication) {
    User author = currentUserService.getCurrentUser(authentication);
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

    if (!hasRole(authentication, "ROLE_ADMIN")
        && !courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, author.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this course");
    }

    CourseAnnouncement announcement =
        courseAnnouncementRepository.save(
            CourseAnnouncement.builder()
                .course(course)
                .createdBy(author)
                .title(request.title().trim())
                .content(request.content().trim())
                .build());
    return courseAnnouncementMapper.toResponse(announcement);
  }

  private boolean hasRole(Authentication authentication, String role) {
    return authentication.getAuthorities().stream()
        .anyMatch(authority -> role.equals(authority.getAuthority()));
  }
}
