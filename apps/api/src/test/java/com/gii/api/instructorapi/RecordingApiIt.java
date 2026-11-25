package com.gii.api.instructorapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.service.media.RecordingStorageService;
import com.gii.common.enums.InstructorRole;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.RecordingStatus;
import com.gii.common.repository.live.LiveClassRecordingRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@TestPropertySource(properties = {"recordings.enabled=true", "recordings.s3.bucket=recording-test"})
class RecordingApiIt extends AbstractInstructorApiIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private LiveClassRecordingRepository recordings;
  @Autowired private TransactionTemplate transactions;
  @MockitoBean private RecordingStorageService storage;

  @AfterEach
  void cleanup() {
    recordings.deleteAll();
    cleanupInstructorData();
  }

  @Test
  void bothMethodsUploadAndQueueWithoutExposingTokensInListResponses() throws Exception {
    var teacher = user("Recording teacher", "recording-teacher@example.com");
    var course = course("Recording course", "recording-course", teacher, PublishStatus.PUBLISHED);
    assignment(course, teacher, InstructorRole.PRIMARY);
    var section = section(course, 1, PublishStatus.PUBLISHED);
    var live =
        liveClass(
            course,
            section,
            null,
            teacher,
            LiveClassStatus.LIVE,
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(3600));
    live.setProvider(LiveClassProvider.GOOGLE_MEET);
    liveClassRepository.saveAndFlush(live);
    when(storage.begin(any())).thenReturn("upload-id");
    for (String method : new String[] {"BROWSER", "EXTENSION"}) {
      String result =
          mvc.perform(
                  post("/instructor/live-classes/{id}/recordings", live.getId())
                      .with(authentication(instructorAuth(teacher.getId())))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"method\":\""
                              + method
                              + "\",\"mimeType\":\"video/mp4;codecs=avc1,mp4a.40.2\"}"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      var session = new ObjectMapper().readTree(result);
      UUID id = UUID.fromString(session.path("id").asText());
      String token = session.path("uploadToken").asText();
      mvc.perform(
              post("/recording-uploads/{id}/parts/1", id)
                  .header("X-Recording-Token", "wrong")
                  .param("bytes", "4096"))
          .andExpect(status().isUnauthorized());
      mvc.perform(
              post("/recording-uploads/{id}/complete", id)
                  .header("X-Recording-Token", token)
                  .param("parts", "1")
                  .param("bytes", "1234"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("QUEUED"));
      assertThat(recordings.findById(id).orElseThrow().getStatus())
          .isEqualTo(RecordingStatus.QUEUED);
    }
    mvc.perform(
            get("/instructor/live-classes/{id}/recordings", live.getId())
                .with(authentication(instructorAuth(teacher.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordings.length()").value(2))
        .andExpect(jsonPath("$.recordings[0].uploadToken").doesNotExist());
    mvc.perform(
            get("/instructor/live-classes/{id}/recordings", live.getId())
                .with(authentication(instructorAuth(UUID.randomUUID()))))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/instructor/live-classes/{id}/recordings", live.getId())
                .with(authentication(instructorAuth(teacher.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"BROWSER\",\"mimeType\":\"video/webm\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/live-classes/{id}/recordings", live.getId())
                .with(authentication(instructorAuth(teacher.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    UUID firstClaim = claimForProcessing();
    UUID secondClaim = claimForProcessing();
    assertThat(secondClaim).isNotEqualTo(firstClaim);
    var unavailable = transactions.execute(status -> recordings.claimNext());
    assertThat(unavailable).isEmpty();
    transactions.executeWithoutResult(status -> {
      var recording = recordings.findLocked(firstClaim).orElseThrow();
      recording.setProcessingUntil(Instant.now().minusSeconds(5));
    });
    transactions.executeWithoutResult(status -> {
      var recovered = recordings.claimNext().orElseThrow();
      assertThat(recovered.getId()).isEqualTo(firstClaim);
      recovered.setStatus(RecordingStatus.READY);
      recovered.setDurationSeconds(60.0);
      recovered.setPublishedAt(Instant.now());
      recovered.setProcessingUntil(null);
    });
    mvc.perform(
            get("/live-classes/{id}/recordings", live.getId())
                .with(authentication(instructorAuth(teacher.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].status").value("READY"));
  }

  private UUID claimForProcessing() {
    return transactions.execute(status -> {
      var recording = recordings.claimNext().orElseThrow();
      recording.setStatus(RecordingStatus.PROCESSING);
      recording.setProcessingUntil(Instant.now().plusSeconds(7200));
      recording.setProcessingClaim(UUID.randomUUID());
      return recording.getId();
    });
  }
}
