package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.api.service.media.MuxDirectUploadClient;
import com.gii.common.entity.certificate.Certificate;
import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.entity.user.UserRole;
import com.gii.common.entity.user.UserRoleId;
import com.gii.common.enums.CertificateTargetType;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.MediaStatus;
import com.gii.common.enums.MuxUploadStatus;
import com.gii.common.enums.SectionItemType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(
    properties = {
      "storage.r2.account-id=test-account",
      "storage.r2.access-key-id=test-access",
      "storage.r2.secret-access-key=test-secret",
      "storage.r2.bucket=test-bucket"
    })
class AdminNewCapabilitiesApiIt extends AbstractAdminApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private MuxDirectUploadClient muxDirectUploadClient;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void adminCanRevokeCertificateWithAuditDataAndRepeatIsIdempotent() throws Exception {
    var admin = user("Certificate Admin", "certificate-revoke-admin@example.com");
    var student = user("Certificate Student", "certificate-revoke-student@example.com");
    var creator = user("Certificate Creator", "certificate-revoke-creator@example.com");
    var course = course("Certificate Course", "certificate-revoke-course", creator);
    Certificate certificate =
        certificateRepository.saveAndFlush(
            Certificate.builder()
                .certificateCode("GII-REVOKE-0001")
                .user(student)
                .targetType(CertificateTargetType.COURSE)
                .course(course)
                .recipientName(student.getFullName())
                .targetTitle(course.getTitle())
                .targetSlug(course.getSlug())
                .issuedBy(creator)
                .build());

    mockMvc
        .perform(
            post("/admin/certificates/{certificateId}/revoke", certificate.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"  duplicate issuance  \"}"))
        .andExpect(status().isNoContent());

    Certificate revoked = certificateRepository.findById(certificate.getId()).orElseThrow();
    assertThat(revoked.getRevokedAt()).isNotNull();
    assertThat(revoked.getRevokedBy().getId()).isEqualTo(admin.getId());
    assertThat(revoked.getRevocationReason()).isEqualTo("duplicate issuance");
    Instant firstRevokedAt = revoked.getRevokedAt();

    mockMvc
        .perform(
            post("/admin/certificates/{certificateId}/revoke", certificate.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"changed\"}"))
        .andExpect(status().isNoContent());
    assertThat(certificateRepository.findById(certificate.getId()).orElseThrow().getRevokedAt())
        .isEqualTo(firstRevokedAt);
  }

  @Test
  void adminCanUploadCreateUpdateAndDeleteLessonResourceMetadata() throws Exception {
    var admin = user("Resource Admin", "resource-admin@example.com");
    var creator = user("Creator", "resource-creator@example.com");
    var course = course("Resources", "resources", creator);
    var section = section(course, 1);
    var lesson = lesson(course, section, 1);

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/resources/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "filename":"handout.pdf",
                      "contentType":"application/pdf",
                      "sizeBytes":1024
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.objectKey")
                .value(
                    org.hamcrest.Matchers.matchesPattern(
                        "lesson-resources/" + lesson.getId() + "/handout-[0-9a-f]{8}\\.pdf")))
        .andExpect(jsonPath("$.method").value("PUT"))
        .andExpect(jsonPath("$.uploadUrl").isNotEmpty());

    String objectKey = "lesson-resources/" + lesson.getId() + "/handout-abcdef12.pdf";
    String createResponse =
        mockMvc
            .perform(
                post("/admin/lessons/{lessonId}/resources", lesson.getId())
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title":"Handout",
                          "titleEn":"Handout EN",
                          "resourceType":"PDF",
                          "mimeType":"application/pdf",
                          "objectKey":"%s",
                          "position":1
                        }
                        """
                            .formatted(objectKey)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.objectKey").value(objectKey))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID resourceId =
        UUID.fromString(
            com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(createResponse)
                .get("resourceId")
                .asText());

    mockMvc
        .perform(
            patch("/admin/lesson-resources/{resourceId}", resourceId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Updated handout"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated handout"));

    mockMvc
        .perform(
            get("/admin/lessons/{lessonId}", lesson.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lessonId").value(lesson.getId().toString()))
        .andExpect(jsonPath("$.primaryResource").doesNotExist())
        .andExpect(jsonPath("$.resources[0].resourceId").value(resourceId.toString()))
        .andExpect(jsonPath("$.resources[0].title").value("Updated handout"));

    mockMvc
        .perform(
            get("/admin/lesson-resources/{resourceId}/download-url", resourceId)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.downloadUrl").isNotEmpty())
        .andExpect(jsonPath("$.fileName").value("Updated handout.pdf"));

    mockMvc
        .perform(
            get("/admin/lessons/{lessonId}", lesson.getId())
                .with(authentication(studentAuthentication())))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/admin/lesson-resources/{resourceId}/download-url", resourceId)
                .with(authentication(studentAuthentication())))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/resources", lesson.getId())
                .with(authentication(studentAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Leaked",
                      "resourceType":"PDF",
                      "mimeType":"application/pdf",
                      "objectKey":"%s",
                      "position":2
                    }
                    """
                        .formatted(objectKey)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/admin/lesson-resources/{resourceId}", resourceId)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());
    assertThat(lessonResourceRepository.existsById(resourceId)).isFalse();
  }

  @Test
  void lessonResourceUploadRejectsOversizeMimeMismatchAndForeignObjectKeys() throws Exception {
    var admin = user("Resource Edge Admin", "resource-edge-admin@example.com");
    var creator = user("Creator", "resource-edge-creator@example.com");
    var course = course("Resource Edge", "resource-edge", creator);
    var lesson = lesson(course, section(course, 1), 1);

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/resources/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "filename":"handout.pdf",
                      "contentType":"image/png",
                      "sizeBytes":1024
                    }
                    """))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/resources/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "filename":"handout.pdf",
                      "contentType":"application/pdf",
                      "sizeBytes":52428801
                    }
                    """))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/resources", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Foreign",
                      "resourceType":"PDF",
                      "mimeType":"application/pdf",
                      "objectKey":"lesson-resources/00000000-0000-0000-0000-000000000000/file-abcdef12.pdf",
                      "position":1
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminCanCreateMuxUploadAndRegisterProcessedVideo() throws Exception {
    var admin = user("Video Admin", "video-admin@example.com");
    var creator = user("Video Creator", "video-creator@example.com");
    var course = course("Video Course", "video-course", creator);
    var lesson = lesson(course, section(course, 1), 1);
    when(muxDirectUploadClient.create(lesson.getId().toString(), lesson.getTitle()))
        .thenReturn(
            new MuxDirectUploadClient.DirectUpload(
                "upload-123", "https://storage.mux.test/upload-123", 3600, "waiting", null));

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/video/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "filename":"introduction.mp4",
                      "contentType":"video/mp4",
                      "sizeBytes":1048576
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.uploadId").value("upload-123"))
        .andExpect(jsonPath("$.method").value("PUT"))
        .andExpect(jsonPath("$.contentType").value("video/mp4"))
        .andExpect(jsonPath("$.uploadUrl").value("https://storage.mux.test/upload-123"));

    mockMvc
        .perform(
            post("/admin/media-assets")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "lessonId":"%s",
                      "provider":"MUX",
                      "assetType":"VIDEO",
                      "providerAssetId":"asset-123",
                      "playbackId":"playback-123",
                      "playbackPolicy":"SIGNED",
                      "title":"Introduction"
                    }
                    """
                        .formatted(lesson.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("MUX"))
        .andExpect(jsonPath("$.providerAssetId").value("asset-123"))
        .andExpect(jsonPath("$.playbackId").value("playback-123"))
        .andExpect(jsonPath("$.playbackPolicy").value("SIGNED"));

    var asset = mediaAssetRepository.findByLessonId(lesson.getId()).orElseThrow();
    assertThat(asset.getProviderAssetId()).isEqualTo("asset-123");
  }

  @Test
  void muxWebhookVerifiesReplaysAndCreatesTheMappedLessonAsset() throws Exception {
    var admin = user("Webhook Admin", "webhook-admin@example.com");
    var creator = user("Webhook Creator", "webhook-creator@example.com");
    var course = course("Webhook Course", "webhook-course", creator);
    var lesson = lesson(course, section(course, 1), 1);
    when(muxDirectUploadClient.create(lesson.getId().toString(), lesson.getTitle()))
        .thenReturn(
            new MuxDirectUploadClient.DirectUpload(
                "upload-webhook",
                "https://storage.mux.test/upload-webhook",
                3600,
                "waiting",
                null));

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/video/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"filename":"lesson.mp4","contentType":"video/mp4","sizeBytes":4096}
                    """))
        .andExpect(status().isOk());

    var tracked = muxVideoUploadRepository.findByUploadId("upload-webhook").orElseThrow();
    assertThat(tracked.getLesson().getId()).isEqualTo(lesson.getId());
    assertThat(tracked.getStatus()).isEqualTo(MuxUploadStatus.WAITING);

    String createdEvent =
        """
        {"id":"evt-created","type":"video.upload.asset_created","data":{"id":"upload-webhook","asset_id":"asset-webhook","passthrough":"%s"}}
        """
            .formatted(lesson.getId())
            .trim();
    long createdTimestamp = Instant.now().getEpochSecond();
    postMuxWebhook(createdEvent, createdTimestamp, muxSignature(createdEvent, createdTimestamp))
        .andExpect(status().isNoContent());
    tracked = muxVideoUploadRepository.findByUploadId("upload-webhook").orElseThrow();
    assertThat(tracked.getAssetId()).isEqualTo("asset-webhook");
    assertThat(tracked.getStatus()).isEqualTo(MuxUploadStatus.PROCESSING);

    String readyEvent =
        """
        {"id":"evt-ready","type":"video.asset.ready","data":{"id":"asset-webhook","upload_id":"upload-webhook","passthrough":"%s","duration":125.6,"resolution_tier":"1080p","playback_ids":[{"id":"public-id","policy":"public"},{"id":"signed-id","policy":"signed"}]}}
        """
            .formatted(lesson.getId())
            .trim();
    long readyTimestamp = Instant.now().getEpochSecond();
    postMuxWebhook(readyEvent, readyTimestamp, muxSignature(readyEvent, readyTimestamp))
        .andExpect(status().isNoContent());
    postMuxWebhook(readyEvent, readyTimestamp, muxSignature(readyEvent, readyTimestamp))
        .andExpect(status().isNoContent());

    tracked = muxVideoUploadRepository.findByUploadId("upload-webhook").orElseThrow();
    assertThat(tracked.getStatus()).isEqualTo(MuxUploadStatus.READY);
    assertThat(tracked.getPlaybackId()).isEqualTo("signed-id");
    assertThat(muxWebhookEventRepository.count()).isEqualTo(2);
    assertThat(mediaAssetRepository.count()).isEqualTo(1);
    var asset = mediaAssetRepository.findByLessonId(lesson.getId()).orElseThrow();
    assertThat(asset.getProviderAssetId()).isEqualTo("asset-webhook");
    assertThat(asset.getPlaybackId()).isEqualTo("signed-id");
    assertThat(asset.getStatus()).isEqualTo(MediaStatus.READY);
    assertThat(asset.getDurationSec()).isEqualTo(126);
    assertThat(lessonRepository.findById(lesson.getId()).orElseThrow().getDurationSeconds())
        .isEqualTo(126);

    String lateCreatedEvent =
        """
        {"id":"evt-created-late","type":"video.upload.asset_created","data":{"id":"upload-webhook","asset_id":"asset-webhook","passthrough":"%s"}}
        """
            .formatted(lesson.getId())
            .trim();
    long lateCreatedTimestamp = Instant.now().getEpochSecond();
    postMuxWebhook(
            lateCreatedEvent,
            lateCreatedTimestamp,
            muxSignature(lateCreatedEvent, lateCreatedTimestamp))
        .andExpect(status().isNoContent());
    assertThat(muxVideoUploadRepository.findByUploadId("upload-webhook").orElseThrow().getStatus())
        .isEqualTo(MuxUploadStatus.READY);
  }

  @Test
  void muxWebhookRejectsForgeryAndStaleSignaturesAndRecordsFailures() throws Exception {
    var admin = user("Failed Webhook Admin", "failed-webhook-admin@example.com");
    var creator = user("Failed Webhook Creator", "failed-webhook-creator@example.com");
    var course = course("Failed Webhook", "failed-webhook", creator);
    var lesson = lesson(course, section(course, 1), 1);
    when(muxDirectUploadClient.create(lesson.getId().toString(), lesson.getTitle()))
        .thenReturn(
            new MuxDirectUploadClient.DirectUpload(
                "upload-failed", "https://storage.mux.test/upload-failed", 3600, "waiting", null));
    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/video/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"filename":"failed.mp4","contentType":"video/mp4","sizeBytes":4096}
                    """))
        .andExpect(status().isOk());

    String failedEvent =
        """
        {"id":"evt-failed","type":"video.upload.errored","data":{"id":"upload-failed","passthrough":"%s","errors":{"type":"invalid_media","message":"Unreadable input"}}}
        """
            .formatted(lesson.getId())
            .trim();
    long now = Instant.now().getEpochSecond();
    postMuxWebhook(failedEvent, now, "00").andExpect(status().isUnauthorized());
    postMuxWebhook(failedEvent, now - 301, muxSignature(failedEvent, now - 301))
        .andExpect(status().isUnauthorized());
    assertThat(muxWebhookEventRepository.count()).isZero();
    assertThat(muxVideoUploadRepository.findByUploadId("upload-failed").orElseThrow().getStatus())
        .isEqualTo(MuxUploadStatus.WAITING);

    postMuxWebhook(failedEvent, now, muxSignature(failedEvent, now))
        .andExpect(status().isNoContent());
    var failed = muxVideoUploadRepository.findByUploadId("upload-failed").orElseThrow();
    assertThat(failed.getStatus()).isEqualTo(MuxUploadStatus.FAILED);
    assertThat(failed.getErrorMessage()).contains("invalid_media", "Unreadable input");
  }

  private org.springframework.test.web.servlet.ResultActions postMuxWebhook(
      String body, long timestamp, String signature) throws Exception {
    return mockMvc.perform(
        post("/webhooks/mux")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Mux-Signature", "t=" + timestamp + ",v1=" + signature)
            .content(body));
  }

  private String muxSignature(String body, long timestamp) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(
        new SecretKeySpec(
            "test-mux-webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of()
        .formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void lessonVideoUploadRejectsMimeMismatchAndNonVideoLesson() throws Exception {
    var admin = user("Video Edge Admin", "video-edge-admin@example.com");
    var creator = user("Video Edge Creator", "video-edge-creator@example.com");
    var course = course("Video Edge", "video-edge", creator);
    var lesson = lesson(course, section(course, 1), 1);

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/video/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"filename":"video.mp4","contentType":"video/webm","sizeBytes":1024}
                    """))
        .andExpect(status().isBadRequest());

    lesson.setLessonType(com.gii.common.enums.LessonType.PDF);
    lessonRepository.saveAndFlush(lesson);
    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/video/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"filename":"video.mp4","contentType":"video/mp4","sizeBytes":1024}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void lessonVideoUploadRejectsNonAdminImmutableCurriculumAndInvalidMuxResponse() throws Exception {
    var admin = user("Hostile Video Admin", "hostile-video-admin@example.com");
    var creator = user("Hostile Video Creator", "hostile-video-creator@example.com");
    var course = course("Hostile Video", "hostile-video", creator);
    var lesson = lesson(course, section(course, 1), 1);
    String request =
        """
        {"filename":"video.mp4","contentType":"video/mp4","sizeBytes":1024}
        """;

    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/video/upload-url", lesson.getId())
                .with(authentication(studentAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isForbidden());

    course.getTemplateVersion().setStatus(com.gii.common.enums.PublishStatus.PUBLISHED);
    courseTemplateVersionRepository.saveAndFlush(course.getTemplateVersion());
    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/video/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isConflict());

    course.getTemplateVersion().setStatus(com.gii.common.enums.PublishStatus.DRAFT);
    courseTemplateVersionRepository.saveAndFlush(course.getTemplateVersion());
    when(muxDirectUploadClient.create(lesson.getId().toString(), lesson.getTitle()))
        .thenReturn(
            new MuxDirectUploadClient.DirectUpload(
                "upload-unsafe", "http://storage.mux.test/upload", -1, "waiting", null));
    mockMvc
        .perform(
            post("/admin/lessons/{lessonId}/video/upload-url", lesson.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isBadGateway());

    mockMvc
        .perform(
            post("/admin/media-assets")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "lessonId":"%s",
                      "provider":"MUX",
                      "assetType":"VIDEO",
                      "title":"Incomplete upload"
                    }
                    """
                        .formatted(lesson.getId())))
        .andExpect(status().isBadRequest());

    lesson.setLessonType(com.gii.common.enums.LessonType.PDF);
    lessonRepository.saveAndFlush(lesson);
    mockMvc
        .perform(
            post("/admin/media-assets")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "lessonId":"%s",
                      "provider":"MUX",
                      "assetType":"VIDEO",
                      "providerAssetId":"asset-unsafe",
                      "playbackId":"playback-unsafe",
                      "playbackPolicy":"SIGNED",
                      "title":"Wrong lesson type"
                    }
                    """
                        .formatted(lesson.getId())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void quizLessonCreationIsRejectedForStandaloneSectionQuiz() throws Exception {
    var admin = user("Structure Admin", "structure-admin@example.com");
    var creator = user("Creator", "structure-creator@example.com");
    var course = course("Structure", "structure", creator);
    var section = section(course, 1);

    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/lessons", section.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Legacy quiz lesson",
                      "slug":"legacy-quiz-lesson",
                      "position":1,
                      "lessonType":"QUIZ"
                    }
                    """))
        .andExpect(status().isBadRequest());

    assertThat(lessonRepository.count()).isZero();
    assertThat(sectionItemRepository.count()).isZero();
  }

  @Test
  void quizDeletePreservesAttemptHistoryAndDeletesOnlyAttemptFreeQuiz() throws Exception {
    var admin = user("Quiz Admin", "delete-quiz-admin@example.com");
    final var student = user("Student", "delete-quiz-student@example.com");
    var creator = user("Creator", "delete-quiz-creator@example.com");
    var course = course("Quiz Delete", "quiz-delete", creator);
    section(course, 1);
    var deletable = quiz(course, "No attempts");

    mockMvc
        .perform(
            delete("/admin/quizzes/{quizId}", deletable.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());
    assertThat(quizRepository.existsById(deletable.getId())).isFalse();
    assertThat(
            sectionItemRepository.findByItemTypeAndItemId(SectionItemType.QUIZ, deletable.getId()))
        .isEmpty();

    var protectedQuiz = quiz(course, "Has attempt");
    var protectedEnrollment = enrollment(student, course, EnrollmentStatus.ACTIVE);
    Instant attemptStartedAt = Instant.now().minusSeconds(1);
    quizAttemptRepository.save(
        QuizAttempt.builder()
            .quiz(protectedQuiz)
            .user(student)
            .enrollment(protectedEnrollment)
            .attemptNo(1)
            .scorePct(50)
            .passed(false)
            .startedAt(attemptStartedAt)
            .submittedAt(Instant.now())
            .build());

    mockMvc
        .perform(
            delete("/admin/quizzes/{quizId}", protectedQuiz.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isConflict());
    assertThat(quizRepository.existsById(protectedQuiz.getId())).isTrue();
    assertThat(quizAttemptRepository.existsByQuizId(protectedQuiz.getId())).isTrue();
  }

  @Test
  void instructorDeleteRemovesCapabilityButPreservesUserAccount() throws Exception {
    var admin = user("Instructor Admin", "delete-instructor-admin@example.com");
    var instructor = user("Instructor", "delete-instructor@example.com");
    var creator = user("Creator", "delete-instructor-creator@example.com");
    var course = course("Instructor Course", "instructor-course", creator);
    final var profile = instructorProfile(instructor);
    assignment(course, instructor);
    var role = roleRepository.findByName("INSTRUCTOR").orElseThrow();
    userRoleRepository.save(
        UserRole.builder()
            .id(UserRoleId.builder().userId(instructor.getId()).roleId(role.getId()).build())
            .user(instructor)
            .role(role)
            .build());

    mockMvc
        .perform(
            delete("/admin/instructors/{instructorId}", instructor.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());

    assertThat(userRepository.existsById(instructor.getId())).isTrue();
    assertThat(instructorProfileRepository.existsById(profile.getUserId())).isFalse();
    assertThat(courseInstructorRepository.findByInstructorId(instructor.getId())).isEmpty();
    assertThat(userRoleRepository.existsByUserIdAndRoleName(instructor.getId(), "INSTRUCTOR"))
        .isFalse();
  }

  @Test
  void adminCanListCloseAndReopenSupportTicketsWhileStudentCannot() throws Exception {
    var admin = user("Support Admin", "support-admin@example.com");

    String created =
        mockMvc
            .perform(
                post("/public/support/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email":"customer@example.com",
                          "subject":"Payment help",
                          "message":"Please help"
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID ticketId =
        UUID.fromString(
            com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(created)
                .get("ticketId")
                .asText());

    mockMvc
        .perform(get("/admin/support/tickets").with(authentication(studentAuthentication())))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/admin/support/tickets").with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ticketId").value(ticketId.toString()))
        .andExpect(jsonPath("$[0].message").value("Please help"));

    mockMvc
        .perform(
            patch("/admin/support/tickets/{ticketId}", ticketId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"CLOSED"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"))
        .andExpect(jsonPath("$.closedAt").isNotEmpty());

    mockMvc
        .perform(
            patch("/admin/support/tickets/{ticketId}", ticketId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"OPEN"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.closedAt").doesNotExist());
  }

  private UsernamePasswordAuthenticationToken studentAuthentication() {
    return new UsernamePasswordAuthenticationToken(
        UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }
}
