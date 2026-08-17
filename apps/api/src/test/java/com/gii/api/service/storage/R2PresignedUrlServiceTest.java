package com.gii.api.service.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class R2PresignedUrlServiceTest {

  @Test
  void addsMimeExtensionWithoutDuplicatingExistingExtension() {
    assertThat(R2PresignedUrlService.resolveDownloadFileName("Lesson Notes", "application/pdf"))
        .isEqualTo("Lesson Notes.pdf");
    assertThat(R2PresignedUrlService.resolveDownloadFileName("Lesson Notes.PDF", "application/pdf"))
        .isEqualTo("Lesson Notes.PDF");
    assertThat(R2PresignedUrlService.resolveDownloadFileName("Diagram", "image/png"))
        .isEqualTo("Diagram.png");
    assertThat(R2PresignedUrlService.resolveDownloadFileName("Photo.jpeg", "image/jpeg"))
        .isEqualTo("Photo.jpeg");
  }
}
