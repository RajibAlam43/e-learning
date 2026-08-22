package com.gii.api.service.student;

import com.gii.api.mapper.CourseAnnouncementMapper;
import com.gii.api.model.response.CourseAnnouncementResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.course.CourseAnnouncement;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.course.CourseAnnouncementRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCourseAnnouncementsService {

  private static final int MAX_PAGE_SIZE = 50;

  private final CurrentUserService currentUserService;
  private final CourseAnnouncementRepository courseAnnouncementRepository;
  private final CourseAnnouncementMapper courseAnnouncementMapper;

  public PageResponse<CourseAnnouncementResponse> execute(
      Pageable pageable, Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    Pageable safePageable =
        PageRequest.of(
            Math.max(pageable.getPageNumber(), 0),
            Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE),
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    Page<CourseAnnouncement> announcements =
        courseAnnouncementRepository.findForStudent(
            userId, EnrollmentStatus.ACTIVE, Instant.now(), safePageable);

    return PageResponse.<CourseAnnouncementResponse>builder()
        .content(
            announcements.getContent().stream().map(courseAnnouncementMapper::toResponse).toList())
        .page(announcements.getNumber())
        .size(announcements.getSize())
        .totalElements(announcements.getTotalElements())
        .totalPages(announcements.getTotalPages())
        .build();
  }
}
