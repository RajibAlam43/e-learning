package com.gii.api.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gii.api.service.storage.R2PresignedUrlService;
import com.gii.common.entity.course.LessonResource;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.LessonResourceRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminLessonResourceManagementServiceTest {

  @Mock private LessonRepository lessonRepository;
  @Mock private LessonResourceRepository lessonResourceRepository;
  @Mock private R2PresignedUrlService r2PresignedUrlService;
  @InjectMocks private AdminLessonResourceManagementService service;

  @Test
  void adminDownloadUsesStoredObjectAndPdfFilename() {
    UUID resourceId = UUID.randomUUID();
    LessonResource resource =
        LessonResource.builder()
            .title("Lesson Notes")
            .mimeType("application/pdf")
            .fileObjectKey("lesson-resources/lesson/notes-abcdef12.pdf")
            .build();
    when(lessonResourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
    when(r2PresignedUrlService.generateDownloadUrl(
            resource.getFileObjectKey(), "Lesson Notes.pdf", resource.getMimeType()))
        .thenReturn(
            new R2PresignedUrlService.PresignedDownload(
                "https://signed.test/notes", Instant.now().plusSeconds(600)));

    var response = service.download(resourceId);

    assertThat(response.downloadUrl()).isEqualTo("https://signed.test/notes");
    assertThat(response.fileName()).isEqualTo("Lesson Notes.pdf");
  }
}
