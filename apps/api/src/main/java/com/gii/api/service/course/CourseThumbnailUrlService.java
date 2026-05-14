package com.gii.api.service.course;

import com.gii.common.entity.course.Course;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CourseThumbnailUrlService {
  private final String assetsBaseUrl;

  public CourseThumbnailUrlService(@Value("${assets.base-url}") String assetsBaseUrl) {
    this.assetsBaseUrl = assetsBaseUrl;
  }

  public String buildCourseThumbnailUrl(Course course) {
    if (course == null) {
      return null;
    }
    return buildCourseThumbnailUrl(course.getId(), course.getThumbnailObjectKey());
  }

  public String buildCourseThumbnailUrl(UUID courseId, String thumbnailObjectKey) {
    if (courseId == null
        || thumbnailObjectKey == null
        || thumbnailObjectKey.isBlank()
        || assetsBaseUrl == null
        || assetsBaseUrl.isBlank()) {
      return null;
    }

    return assetsBaseUrl + "/courses/" + courseId + "/" + thumbnailObjectKey.trim();
  }
}
