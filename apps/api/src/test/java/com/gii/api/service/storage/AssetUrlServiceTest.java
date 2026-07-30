package com.gii.api.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AssetUrlServiceTest {

  private final AssetUrlService service =
      new AssetUrlService("https://assets.example.com/");

  @Test
  void shouldResolveFullObjectKeyAndNormalizeBlank() {
    assertThat(service.publicUrl("courses/id/thumbnails/image.webp"))
        .isEqualTo("https://assets.example.com/courses/id/thumbnails/image.webp");
    assertThat(service.publicUrl(" ")).isNull();
    assertThat(service.normalizeThumbnailKey(" ", "courses", UUID.randomUUID())).isNull();
  }

  @Test
  void shouldAcceptOnlyThumbnailKeyOwnedByEntity() {
    UUID courseId = UUID.randomUUID();
    String key = "courses/" + courseId + "/thumbnails/" + UUID.randomUUID() + ".webp";

    assertThat(service.normalizeThumbnailKey(key, "courses", courseId)).isEqualTo(key);

    assertThatThrownBy(
            () -> service.normalizeThumbnailKey(key, "courses", UUID.randomUUID()))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldRejectUrlsTraversalAndUnsupportedExtensions() {
    UUID lessonId = UUID.randomUUID();
    String prefix = "lessons/" + lessonId + "/thumbnails/";

    assertInvalid("https://assets.example.com/" + prefix + "image.webp", lessonId);
    assertInvalid(prefix + "../image.webp", lessonId);
    assertInvalid(prefix + "image.svg", lessonId);
  }

  private void assertInvalid(String key, UUID lessonId) {
    assertThatThrownBy(() -> service.normalizeThumbnailKey(key, "lessons", lessonId))
        .isInstanceOf(ResponseStatusException.class);
  }
}
