package com.gii.api.service.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gii.api.model.request.instructor.StartRecordingRequest;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassRecording;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.RecordingMethod;
import com.gii.common.enums.RecordingStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.live.LiveClassRecordingRepository;
import com.gii.common.repository.live.LiveClassRepository;
import com.gii.common.repository.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class LiveClassRecordingServiceTest {
  private final LiveClassRepository classes = mock(LiveClassRepository.class);
  private final LiveClassRecordingRepository recordings = mock(LiveClassRecordingRepository.class);
  private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
  private final RecordingStorageService storage = mock(RecordingStorageService.class);
  private final RecordingPlaybackService playback = mock(RecordingPlaybackService.class);
  private final UUID teacherId = UUID.randomUUID();
  private final UUID classId = UUID.randomUUID();
  private final UUID courseId = UUID.randomUUID();
  private final Authentication teacher = auth(teacherId, "INSTRUCTOR");
  private final LiveClass liveClass = mock(LiveClass.class);
  private LiveClassRecordingService service;
  private LiveClassRecording saved;

  @BeforeEach
  void setup() {
    service =
        new LiveClassRecordingService(
            new CurrentUserService(mock(UserRepository.class)),
            classes,
            recordings,
            enrollments,
            storage,
            playback);
    ReflectionTestUtils.setField(service, "enabled", true);
    ReflectionTestUtils.setField(service, "bucket", "private-recordings");
    when(liveClass.getId()).thenReturn(classId);
    when(liveClass.getProvider()).thenReturn(LiveClassProvider.GOOGLE_MEET);
    when(liveClass.getStatus()).thenReturn(LiveClassStatus.LIVE);
    when(liveClass.getCourse())
        .thenReturn(Course.builder().id(courseId).status(PublishStatus.PUBLISHED).build());
    when(liveClass.getSection())
        .thenReturn(CourseSection.builder().status(PublishStatus.PUBLISHED).build());
    when(classes.findById(classId)).thenReturn(Optional.of(liveClass));
    when(classes.findByIdAssignedToInstructor(classId, teacherId))
        .thenReturn(Optional.of(liveClass));
    when(storage.begin(any())).thenReturn("s3-upload-id");
    when(recordings.saveAndFlush(any()))
        .thenAnswer(call -> {
          saved = call.getArgument(0);
          if (saved.getId() == null) {
            saved.setId(UUID.randomUUID());
          }
          saved.setCreatedAt(Instant.now());
          when(recordings.findLocked(saved.getId())).thenAnswer(ignored -> Optional.of(saved));
          return saved;
        });
  }

  @ParameterizedTest
  @EnumSource(RecordingMethod.class)
  void createsScopedUploadsForBothMethods(RecordingMethod method) {
    var session =
        service.start(
            classId, new StartRecordingRequest(method, "video/mp4;codecs=avc1,mp4a.40.2"), teacher);
    assertThat(saved.getMethod()).isEqualTo(method);
    assertThat(saved.getUploadTokenHash()).isNotEqualTo(session.uploadToken());
    assertThat(session.uploadToken()).hasSize(43);
    assertThat(session.partSizeBytes()).isEqualTo(8L * 1024 * 1024);
    assertThat(saved.getObjectPrefix()).startsWith("recordings/" + classId + "/");
    service.part(session.id(), 1, 4096, session.uploadToken());
    verify(storage).partUrl(saved.getObjectPrefix() + "source.mp4", "s3-upload-id", 1, 4096);
  }

  @Test
  void rejectsUnknownAndExpiredUploadTokensBeforeS3Access() {
    var session = start();
    assertThatThrownBy(() -> service.part(session.id(), 1, 4096, "wrong-token"))
        .isInstanceOf(ResponseStatusException.class);
    saved.setUploadExpiresAt(Instant.now().minusSeconds(1));
    assertThatThrownBy(() -> service.part(session.id(), 1, 4096, session.uploadToken()))
        .isInstanceOf(ResponseStatusException.class);
    verify(storage, never()).partUrl(any(), any(), anyInt(), anyLong());
  }

  @Test
  void renewingAuthorizationInvalidatesPreviousToken() {
    var original = start();
    var renewed = service.resume(original.id(), teacher);
    assertThatThrownBy(() -> service.part(original.id(), 1, 4096, original.uploadToken()))
        .isInstanceOf(ResponseStatusException.class);
    service.part(renewed.id(), 1, 4096, renewed.uploadToken());
  }

  @Test
  void revokedTeacherCannotContinueUploading() {
    var session = start();
    when(classes.findByIdAssignedToInstructor(classId, teacherId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.part(session.id(), 1, 4096, session.uploadToken()))
        .isInstanceOf(ResponseStatusException.class);
    verify(storage, never()).partUrl(any(), any(), anyInt(), anyLong());
  }

  @Test
  void completionIsIdempotentAndOnlyQueuesAfterStorageVerification() {
    var session = start();
    assertThat(service.complete(session.id(), 1, 4096, session.uploadToken()).status())
        .isEqualTo(RecordingStatus.QUEUED);
    assertThat(service.complete(session.id(), 1, 4096, session.uploadToken()).status())
        .isEqualTo(RecordingStatus.QUEUED);
    verify(storage, times(1)).complete(any(), any(), eq(1), eq(4096L));
    assertThatThrownBy(() -> service.complete(session.id(), 1, 42, session.uploadToken()))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.part(session.id(), 2, 4096, session.uploadToken()))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void incompleteUploadsRemainUnpublished() {
    var session = start();
    doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "missing part"))
        .when(storage)
        .complete(any(), any(), anyInt(), anyLong());
    assertThatThrownBy(() -> service.complete(session.id(), 1, 4096, session.uploadToken()))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(saved.getStatus()).isEqualTo(RecordingStatus.UPLOADING);
    verifyNoInteractions(playback);
  }

  @Test
  void expiredEnrollmentAndUnreadyMediaCannotReceivePlayback() {
    var session = start();
    when(recordings.findById(session.id())).thenAnswer(ignored -> Optional.of(saved));
    UUID studentId = UUID.randomUUID();
    Authentication student = auth(studentId, "STUDENT");
    when(enrollments.findByUserIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE))
        .thenReturn(
            Optional.of(Enrollment.builder().expiresAt(Instant.now().minusSeconds(1)).build()));
    saved.setStatus(RecordingStatus.READY);
    assertThatThrownBy(() ->
            service.playback(
                session.id(), student, new MockHttpServletRequest(), new MockHttpServletResponse()))
        .isInstanceOf(ResponseStatusException.class);
    saved.setStatus(RecordingStatus.PROCESSING);
    assertThatThrownBy(() ->
            service.playback(
                session.id(), teacher, new MockHttpServletRequest(), new MockHttpServletResponse()))
        .isInstanceOf(ResponseStatusException.class);
    verifyNoInteractions(playback);
  }

  @Test
  void publishedListOnlyIncludesReadyMediaForActiveEnrolledStudents() {
    start();
    UUID studentId = UUID.randomUUID();
    when(enrollments.findByUserIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE))
        .thenReturn(Optional.of(Enrollment.builder().build()));
    var processing = LiveClassRecording.builder().status(RecordingStatus.PROCESSING).build();
    saved.setStatus(RecordingStatus.READY);
    when(recordings.findByLiveClassIdOrderByCreatedAtDesc(classId))
        .thenReturn(List.of(saved, processing));
    assertThat(service.published(classId, auth(studentId, "STUDENT"))).hasSize(1);
    assertThatThrownBy(() -> service.published(classId, auth(UUID.randomUUID(), "STUDENT")))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void featureMustBeEnabledAndTeacherAssigned() {
    ReflectionTestUtils.setField(service, "enabled", false);
    assertThatThrownBy(this::start).isInstanceOf(ResponseStatusException.class);
    ReflectionTestUtils.setField(service, "enabled", true);
    when(classes.findByIdAssignedToInstructor(classId, teacherId)).thenReturn(Optional.empty());
    assertThatThrownBy(this::start).isInstanceOf(ResponseStatusException.class);
    verify(storage, never()).begin(any());
  }

  private com.gii.api.model.response.RecordingResponse.UploadSession start() {
    return service.start(
        classId,
        new StartRecordingRequest(RecordingMethod.BROWSER, "video/mp4;codecs=avc1,mp4a.40.2"),
        teacher);
  }

  private static Authentication auth(UUID id, String role) {
    return new UsernamePasswordAuthenticationToken(
        id, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }
}
