package com.gii.api.service.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.gii.common.entity.course.Course;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CourseThumbnailUrlServiceTest {

  @Test
  void shouldBuildUrlFromFilenameAndCourseId() {
    CourseThumbnailUrlService service = new CourseThumbnailUrlService("https://assets.example.com");
    UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    String url = service.buildCourseThumbnailUrl(courseId, "thumbnail-v1.webp");

    assertEquals(
        "https://assets.example.com/courses/123e4567-e89b-12d3-a456-426614174000/thumbnail-v1.webp",
        url);
  }

  @Test
  void shouldBuildUrlWhenBaseAndFilenameAlreadyNormalized() {
    CourseThumbnailUrlService service = new CourseThumbnailUrlService("https://assets.example.com");
    UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    String url = service.buildCourseThumbnailUrl(courseId, "thumbnail-v2.webp");

    assertEquals(
        "https://assets.example.com/courses/123e4567-e89b-12d3-a456-426614174000/thumbnail-v2.webp",
        url);
  }

  @Test
  void shouldReturnNullForInvalidInputs() {
    CourseThumbnailUrlService service = new CourseThumbnailUrlService("https://assets.example.com");
    UUID courseId = UUID.randomUUID();

    assertNull(service.buildCourseThumbnailUrl(null, "thumbnail-v1.webp"));
    assertNull(service.buildCourseThumbnailUrl(courseId, null));
    assertNull(service.buildCourseThumbnailUrl(courseId, " "));
    assertNull(service.buildCourseThumbnailUrl((Course) null));
  }
}
