package com.gii.api.mapper;

import com.gii.api.model.response.CourseAnnouncementResponse;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.common.entity.course.CourseAnnouncement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAnnouncementMapper {

  private final LocalizedContentService localizedContentService;

  public CourseAnnouncementResponse toResponse(CourseAnnouncement announcement) {
    return CourseAnnouncementResponse.builder()
        .announcementId(announcement.getId())
        .courseId(announcement.getCourse().getId())
        .courseTitle(
            localizedContentService.text(
                announcement.getCourse().getTitle(), announcement.getCourse().getTitleEn()))
        .title(announcement.getTitle())
        .content(announcement.getContent())
        .createdAt(announcement.getCreatedAt())
        .build();
  }
}
