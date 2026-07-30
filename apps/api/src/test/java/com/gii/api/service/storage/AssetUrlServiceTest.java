package com.gii.api.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    assertThat(service.normalizeThumbnailKey(" ", "courses")).isNull();
  }

  @Test
  void shouldAcceptGeneratedThumbnailUploadKey() {
    String key = "thumbnails/media-assets/media-cover-a1b2c3d4.webp";
    assertThat(service.normalizeThumbnailKey(key, "media-assets")).isEqualTo(key);
  }

  @Test
  void shouldRejectUrlsTraversalAndUnsupportedExtensions() {
    String prefix = "thumbnails/courses/";
    assertInvalid("https://assets.example.com/" + prefix + "image.webp");
    assertInvalid(prefix + "../image-a1b2c3d4.webp");
    assertInvalid(prefix + "image-a1b2c3d4.svg");
    assertInvalid("course-cover.webp");
    assertInvalid("thumbnails/courses/image.webp");
    assertInvalid("thumbnails/collections/image-a1b2c3d4.webp");
  }

  private void assertInvalid(String key) {
    assertThatThrownBy(() -> service.normalizeThumbnailKey(key, "courses"))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }
}
