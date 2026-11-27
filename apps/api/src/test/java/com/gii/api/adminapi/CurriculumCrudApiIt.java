package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.quiz.QuizAttempt;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.SectionItemType;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CurriculumCrudApiIt extends AbstractAdminApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired EntityManager entities;
  private final ObjectMapper json = new ObjectMapper();

  private UUID slot(User admin, Course c, CourseSection section, int position) throws Exception {
    String result =
        mvc.perform(
                post("/admin/courses/{id}/live-classes", c.getId())
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"sectionId":"%s","title":"Accidental live class","position":%s,
                         "expectedDurationMinutes":30,"isMandatory":true}
                        """
                            .formatted(section.getId(), position)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(json.readTree(result).path("liveClassItemId").asText());
  }

  private String itemPath(Course c, UUID itemId) {
    return "/admin/courses/" + c.getId() + "/live-classes/" + itemId;
  }

  @Test
  void sectionAndSupportedLessonTypesCanBeAddedEditedAndRemoved() throws Exception {
    final var admin = user("Admin", "crud-lessons-admin@example.com");
    final var c = course("Curriculum", "crud-lessons", admin);
    final var response =
        mvc.perform(
                post("/admin/courses/{id}/sections", c.getId())
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Section\",\"slug\":\"crud-section\",\"position\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final var sectionId = UUID.fromString(json.readTree(response).path("sectionId").asText());
    mvc.perform(
            patch("/admin/sections/{id}", sectionId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Edited section\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Edited section"));
    int position = 1;
    for (String type : List.of("VIDEO", "PDF")) {
      final var body =
          mvc.perform(
                  post("/admin/sections/{id}/lessons", sectionId)
                      .with(authentication(adminAuth(admin.getId())))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          """
                          {"title":"%s lesson","slug":"crud-%s","position":%s,"lessonType":"%s"}
                          """
                              .formatted(type, type.toLowerCase(), position++, type)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      final var lessonId = UUID.fromString(json.readTree(body).path("lessonId").asText());
      mvc.perform(
              patch("/admin/lessons/{id}", lessonId)
                  .with(authentication(adminAuth(admin.getId())))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"title\":\"Edited lesson\",\"durationSeconds\":120}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("Edited lesson"));
      mvc.perform(
              delete("/admin/lessons/{id}", lessonId)
                  .with(authentication(adminAuth(admin.getId()))))
          .andExpect(status().isOk());
      assertThat(sectionItemRepository.findByItemTypeAndItemId(SectionItemType.LESSON, lessonId))
          .isEmpty();
    }
    mvc.perform(
            delete("/admin/sections/{id}", sectionId)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    entities.clear();
    assertThat(courseSectionRepository.findById(sectionId)).isEmpty();
    assertThat(courseRepository.findById(c.getId())).isPresent();
  }

  @Test
  void anUnusedSectionCanBeRemovedWithAllItsCurriculumItems() throws Exception {
    final var admin = user("Admin", "crud-section-children@example.com");
    final var c = course("Curriculum", "crud-section-children", admin);
    final var section = section(c, 1);
    final var lesson = lesson(c, section, 1);
    final var video = mediaAsset(lesson, "unused-video");
    final var quiz = quiz(c, "Unused quiz");
    final var question = question(quiz, 1, "Unused question");
    choice(question, "Answer", true);
    final var liveSlot = slot(admin, c, section, 3);
    mvc.perform(
            delete("/admin/sections/{id}", section.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    entities.clear();
    assertThat(courseSectionRepository.findById(section.getId())).isEmpty();
    assertThat(lessonRepository.findById(lesson.getId())).isEmpty();
    assertThat(mediaAssetRepository.findById(video.getId())).isEmpty();
    assertThat(quizRepository.findById(quiz.getId())).isEmpty();
    assertThat(quizQuestionRepository.findById(question.getId())).isEmpty();
    assertThat(liveClassSlotRepository.findById(liveSlot)).isEmpty();
  }

  @Test
  void anUnusedLiveClassCanBeEditedAndRemovedWithoutRemovingItsSibling() throws Exception {
    final var admin = user("Admin", "crud-live-admin@example.com");
    final var c = course("Curriculum", "crud-live", admin);
    final var section = section(c, 1);
    final var first = slot(admin, c, section, 1);
    final var second = slot(admin, c, section, 2);
    mvc.perform(
            put(itemPath(c, first))
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sectionId":"%s","title":"Edited live class","position":1,
                     "description":"Updated description","expectedDurationMinutes":75,"isMandatory":false}
                    """
                        .formatted(section.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Edited live class"))
        .andExpect(jsonPath("$.expectedDurationMinutes").value(75));
    mvc.perform(get(itemPath(c, first)).with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Updated description"));
    mvc.perform(delete(itemPath(c, first)).with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());
    assertThat(liveClassSlotRepository.findById(first)).isEmpty();
    assertThat(liveClassSlotRepository.findById(second)).isPresent();
    assertThat(sectionItemRepository.findByItemTypeAndItemId(SectionItemType.LIVE_CLASS, first))
        .isEmpty();
    mvc.perform(
            get("/admin/courses/{id}", c.getId()).with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].items.length()").value(1));
    verifyNoInteractions(liveMeetingProvisioningService);
  }

  @Test
  void scheduledOngoingAndCompletedMeetingsCannotBeRemoved() throws Exception {
    final var admin = user("Admin", "crud-protected-admin@example.com");
    final var c = course("Curriculum", "crud-protected", admin);
    final var section = section(c, 1);
    for (var state :
        List.of(LiveClassStatus.SCHEDULED, LiveClassStatus.LIVE, LiveClassStatus.COMPLETED)) {
      final var session = liveClass(c, section, null);
      session.setStatus(state);
      liveClassRepository.saveAndFlush(session);
      mvc.perform(
              put(itemPath(c, session.getSlot().getId()))
                  .with(authentication(adminAuth(admin.getId())))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"sectionId":"%s","title":"Updated curriculum title"}
                      """
                          .formatted(section.getId())))
          .andExpect(status().isOk());
      mvc.perform(
              get("/admin/courses/{id}", c.getId()).with(authentication(adminAuth(admin.getId()))))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$.sections[0].items[-1:].liveClass.title")
                  .value(
                      org.hamcrest.Matchers.everyItem(
                          org.hamcrest.Matchers.is("Updated curriculum title"))));
      mvc.perform(
              delete(itemPath(c, session.getSlot().getId()))
                  .with(authentication(adminAuth(admin.getId()))))
          .andExpect(status().isConflict());
      assertThat(liveClassRepository.findById(session.getId())).isPresent();
      assertThat(
              sectionItemRepository.findByItemTypeAndItemId(
                  SectionItemType.LIVE_CLASS, session.getSlot().getId()))
          .isPresent();
    }
  }

  @Test
  void cancelledMeetingHistoryIsRetainedAndCannotBeScheduledAgainAfterRemoval() throws Exception {
    final var admin = user("Admin", "crud-cancelled-admin@example.com");
    final var learner = user("Learner", "crud-cancelled-student@example.com");
    final var c = course("Curriculum", "crud-cancelled", admin);
    final var section = section(c, 1);
    final var session = liveClass(c, section, null);
    session.setStatus(LiveClassStatus.CANCELLED);
    liveClassRepository.saveAndFlush(session);
    final var participant =
        registrant(session, learner, com.gii.common.enums.LiveClassRegistrantStatus.APPROVED);
    mvc.perform(
            delete(itemPath(c, session.getSlot().getId()))
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());
    assertThat(liveClassRepository.findById(session.getId())).isPresent();
    assertThat(liveClassRegistrantRepository.findById(participant.getId())).isPresent();
    assertThat(
            liveClassSlotRepository
                .findById(session.getSlot().getId())
                .orElseThrow()
                .getIsMandatory())
        .isFalse();
    mvc.perform(
            post(itemPath(c, session.getSlot().getId()) + "/schedule-with-url")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"startsAt":"2030-01-01T09:00:00Z","endsAt":"2030-01-01T10:00:00Z",
                     "provider":"OTHER","maxCapacity":10,"participantJoinUrl":"https://meet.example.com/class"}
                    """))
        .andExpect(status().isNotFound());
    mvc.perform(
            delete("/admin/sections/{id}", section.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isConflict());
  }

  @Test
  void anotherCohortsScheduledMeetingAlsoProtectsTheSharedItem() throws Exception {
    final var admin = user("Admin", "crud-cohort-admin@example.com");
    final var c = course("Curriculum", "crud-cohort", admin);
    final var section = section(c, 1);
    final var session = liveClass(c, section, null);
    final var other =
        courseRepository.saveAndFlush(
            Course.builder()
                .template(c.getTemplate())
                .name("Other cohort")
                .slug("crud-other-cohort")
                .createdBy(admin)
                .build());
    session.setCourse(other);
    liveClassRepository.saveAndFlush(session);
    mvc.perform(
            delete(itemPath(c, session.getSlot().getId()))
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isConflict());
  }

  @Test
  void liveClassOrderingUsesTheSameSequenceAsLessonsAndQuizzes() throws Exception {
    final var admin = user("Admin", "crud-order-admin@example.com");
    final var c = course("Curriculum", "crud-order", admin);
    final var section = section(c, 1);
    final var video = lesson(c, section, 1);
    final var quiz = quiz(c, "Quiz");
    final var slotId = slot(admin, c, section, 3);
    mvc.perform(
            put(itemPath(c, slotId))
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sectionId":"%s","title":"Live","position":1}
                    """
                        .formatted(section.getId())))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/admin/courses/{id}/structure/reorder", c.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sections":[{"sectionId":"%s","newPosition":1,"items":[
                    {"itemId":"%s","itemType":"LIVE_CLASS","newPosition":1},
                    {"itemId":"%s","itemType":"QUIZ","newPosition":2},
                    {"itemId":"%s","itemType":"LESSON","newPosition":3}]}]}
                    """
                        .formatted(section.getId(), slotId, quiz.getId(), video.getId())))
        .andExpect(status().isOk());
    mvc.perform(
            get("/admin/courses/{id}", c.getId()).with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].items[0].itemType").value("LIVE_CLASS"))
        .andExpect(jsonPath("$.sections[0].items[2].itemType").value("LESSON"));
  }

  @Test
  void quizQuestionsCanBeReadAndReplacedBeforeAttemptsAndTheQuizCanBeRemoved() throws Exception {
    final var admin = user("Admin", "crud-quiz-admin@example.com");
    final var c = course("Curriculum", "crud-quiz", admin);
    final var quiz = quiz(c, "Quiz");
    final var q = question(quiz, 1, "Old question");
    choice(q, "Old A", true);
    choice(q, "Old B", false);
    mvc.perform(
            get("/admin/quizzes/{id}", quiz.getId()).with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.questions[0].questionText").value("Old question"));
    mvc.perform(
            patch("/admin/quizzes/{id}", quiz.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Edited quiz","questions":[{"position":1,"questionText":"New question",
                     "questionType":"MCQ","points":2,"choices":[
                     {"choiceText":"New A","isCorrect":true},{"choiceText":"New B","isCorrect":false}]}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.questions[0].questionText").value("New question"));
    mvc.perform(
            delete("/admin/quizzes/{id}", quiz.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNoContent());
    assertThat(quizRepository.findById(quiz.getId())).isEmpty();
  }

  @Test
  void recordedQuizAttemptsPreventQuestionReplacementQuizDeletionAndSectionDeletion()
      throws Exception {
    final var admin = user("Admin", "crud-attempt-admin@example.com");
    final var learner = user("Learner", "crud-attempt-student@example.com");
    final var c = course("Curriculum", "crud-attempt", admin);
    final var quiz = quiz(c, "Quiz");
    final var enrollment = enrollment(learner, c, EnrollmentStatus.ACTIVE);
    final var attempt =
        quizAttemptRepository.saveAndFlush(
            QuizAttempt.builder()
                .quiz(quiz)
                .user(learner)
                .enrollment(enrollment)
                .attemptNo(1)
                .build());
    mvc.perform(
            patch("/admin/quizzes/{id}", quiz.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questions\":[]}"))
        .andExpect(status().isConflict());
    mvc.perform(
            delete("/admin/quizzes/{id}", quiz.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isConflict());
    mvc.perform(
            delete("/admin/sections/{id}", quiz.getSection().getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isConflict());
    assertThat(quizAttemptRepository.findById(attempt.getId())).isPresent();
  }

  @Test
  void liveClassChangesRequireAdminAndCorrectCourseOwnership() throws Exception {
    final var admin = user("Admin", "crud-auth-admin@example.com");
    final var c = course("Curriculum", "crud-auth", admin);
    final var other = course("Other", "crud-auth-other", admin);
    final var section = section(c, 1);
    final var slotId = slot(admin, c, section, 1);
    final var student =
        new UsernamePasswordAuthenticationToken(
            admin.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    mvc.perform(delete(itemPath(c, slotId)).with(authentication(student)))
        .andExpect(status().isForbidden());
    mvc.perform(get(itemPath(c, slotId)).with(authentication(student)))
        .andExpect(status().isForbidden());
    final var quiz = quiz(c, "Private quiz answers");
    mvc.perform(get("/admin/quizzes/{id}", quiz.getId()).with(authentication(student)))
        .andExpect(status().isForbidden());
    mvc.perform(delete(itemPath(other, slotId)).with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isBadRequest());
    assertThat(liveClassSlotRepository.findById(slotId)).isPresent();
  }
}
