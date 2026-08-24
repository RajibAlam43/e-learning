package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.course.LessonResource;
import com.gii.common.enums.LessonResourcePurpose;
import com.gii.common.enums.LessonResourceType;
import com.gii.common.enums.LiveClassProvisioningMode;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AdminCourseStructureApiIt extends AbstractAdminApiIntegrationTest {

  @Test
  void adminCourseDetailsIncludeLessonResourceSummaries() throws Exception {
    var admin = user("Resource Summary Admin", "resource-summary-admin@example.com");
    var course = course("Resource Summary", "resource-summary", admin);
    var section = section(course, 1);
    var lesson = lesson(course, section, 1);
    lessonResourceRepository.saveAllAndFlush(
        java.util.List.of(
            LessonResource.builder()
                .lesson(lesson)
                .title("Primary PDF")
                .titleEn("Primary PDF EN")
                .resourceType(LessonResourceType.PDF)
                .purpose(LessonResourcePurpose.PRIMARY_CONTENT)
                .fileUrl("courses/resources/primary.pdf")
                .mimeType("application/pdf")
                .position(1)
                .build(),
            LessonResource.builder()
                .lesson(lesson)
                .title("Worksheet")
                .titleEn("Worksheet EN")
                .resourceType(LessonResourceType.PDF)
                .purpose(LessonResourcePurpose.SUPPLEMENTARY)
                .fileUrl("courses/resources/worksheet.pdf")
                .mimeType("application/pdf")
                .position(2)
                .build()));

    mockMvc
        .perform(
            get("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.sections[0].items[0].lesson.primaryResource.title").value("Primary PDF"))
        .andExpect(jsonPath("$.sections[0].items[0].lesson.resources.length()").value(1))
        .andExpect(jsonPath("$.sections[0].items[0].lesson.resources[0].title").value("Worksheet"))
        .andExpect(jsonPath("$.sections[0].items[0].lesson.resources[0].fileUrl").doesNotExist());
  }

  @Test
  void liveClassCanBeAddedToCurriculumBeforeItIsScheduled() throws Exception {
    var admin = user("Live Item Admin", "live-item-admin@example.com");
    var course = course("Live Item Course", "live-item-course", admin);
    var section = section(course, 1);

    String itemResponse =
        mockMvc
            .perform(
                post("/admin/courses/{courseId}/live-classes", course.getId())
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "sectionId":"%s",
                          "position":1,
                          "title":"Weekly workshop",
                          "expectedDurationMinutes":60
                        }
                        """
                            .formatted(section.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scheduled").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();
    java.util.UUID liveClassItemId =
        java.util.UUID.fromString(
            new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(itemResponse)
                .get("liveClassItemId")
                .asText());

    assertThat(liveClassRepository.findByCourseIdOrderByStartsAtAsc(course.getId())).isEmpty();
    mockMvc
        .perform(
            get("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].items[0].itemType").value("LIVE_CLASS"))
        .andExpect(jsonPath("$.sections[0].items[0].liveClass.scheduled").value(false));

    mockMvc
        .perform(
            post(
                    "/admin/courses/{courseId}/live-classes/{liveClassId}/schedule",
                    course.getId(),
                    liveClassItemId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "startsAt":"2030-10-01T14:00:00Z",
                      "endsAt":"2030-10-01T15:00:00Z",
                      "provider":"ZOOM",
                      "maxCapacity":50
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Weekly workshop"))
        .andExpect(jsonPath("$.status").value("SCHEDULED"));

    assertThat(liveClassRepository.findByCourseIdOrderByStartsAtAsc(course.getId())).hasSize(1);
  }

  @Test
  void externalSchedulingDoesNotCallProvider() throws Exception {
    var admin = user("External Live Admin", "external-live-admin@example.com");
    var course = course("External Live Course", "external-live-course", admin);
    var section = section(course, 1);
    java.util.UUID liveClassItemId =
        createLiveClassItem(admin.getId(), course.getId(), section.getId());

    String response =
        mockMvc
            .perform(
                post(
                        "/admin/courses/{courseId}/live-classes/{liveClassId}/schedule-with-url",
                        course.getId(),
                        liveClassItemId)
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "startsAt":"2099-10-01T14:00:00Z",
                          "endsAt":"2099-10-01T15:00:00Z",
                          "provider":"TEAMS",
                          "participantJoinUrl":"https://teams.example.com/meeting/abc",
                          "maxCapacity":75
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("External workshop"))
            .andExpect(jsonPath("$.provider").value("TEAMS"))
            .andExpect(jsonPath("$.provisioningMode").value("EXTERNAL_URL"))
            .andExpect(jsonPath("$.joinUrl").value("https://teams.example.com/meeting/abc"))
            .andExpect(jsonPath("$.hostStartUrl").value("https://teams.example.com/meeting/abc"))
            .andExpect(jsonPath("$.meetingId").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();

    java.util.UUID scheduledLiveClassId =
        java.util.UUID.fromString(
            new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response)
                .get("liveClassId")
                .asText());
    var saved = liveClassRepository.findById(scheduledLiveClassId).orElseThrow();
    assertThat(saved.getSlot().getId()).isEqualTo(liveClassItemId);
    assertThat(saved.getProvisioningMode()).isEqualTo(LiveClassProvisioningMode.EXTERNAL_URL);
    assertThat(saved.getProviderMeetingId()).isNull();
    verifyNoInteractions(liveMeetingProvisioningService);
  }

  @Test
  void externalUpdateAndCancelDoNotCallProvider() throws Exception {
    var admin = user("External Lifecycle Admin", "external-lifecycle-admin@example.com");
    var course = course("External Lifecycle Course", "external-lifecycle-course", admin);
    var section = section(course, 1);
    java.util.UUID itemId = createLiveClassItem(admin.getId(), course.getId(), section.getId());
    java.util.UUID liveClassId = scheduleExternal(admin.getId(), course.getId(), itemId);

    mockMvc
        .perform(
            patch("/live-classes/{liveClassId}", liveClassId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Updated locally\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Updated locally"));

    mockMvc
        .perform(
            delete("/live-classes/{liveClassId}", liveClassId)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    verifyNoInteractions(liveMeetingProvisioningService);
  }

  @Test
  void externalLiveClassCanStartWithoutProviderMeetingId() throws Exception {
    var admin = user("External Start Admin", "external-start-admin@example.com");
    var course = course("External Start Course", "external-start-course", admin);
    var section = section(course, 1);
    java.util.UUID itemId = createLiveClassItem(admin.getId(), course.getId(), section.getId());
    java.util.UUID liveClassId = scheduleExternal(admin.getId(), course.getId(), itemId);

    mockMvc
        .perform(
            post("/live-classes/{liveClassId}/start", liveClassId)
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LIVE"))
        .andExpect(jsonPath("$.hostStartUrl").value("https://class.example.com/room/abc"))
        .andExpect(jsonPath("$.meetingId").doesNotExist());

    assertThat(liveClassRepository.findById(liveClassId).orElseThrow().getStatus().name())
        .isEqualTo("LIVE");
    verifyNoInteractions(liveMeetingProvisioningService);
  }

  @Test
  void externalLiveClassRequiresAnHttpsParticipantUrl() throws Exception {
    var admin = user("External URL Admin", "external-url-admin@example.com");
    var course = course("External URL Course", "external-url-course", admin);
    var section = section(course, 1);
    java.util.UUID itemId = createLiveClassItem(admin.getId(), course.getId(), section.getId());

    mockMvc
        .perform(
            post(
                    "/admin/courses/{courseId}/live-classes/{liveClassId}/schedule-with-url",
                    course.getId(),
                    itemId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "startsAt":"2099-10-01T14:00:00Z",
                      "endsAt":"2099-10-01T15:00:00Z",
                      "provider":"OTHER",
                      "participantJoinUrl":"http://unsafe.example.com/meeting",
                      "maxCapacity":25
                    }
                    """))
        .andExpect(status().isBadRequest());

    assertThat(liveClassRepository.findByCourseIdOrderByStartsAtAsc(course.getId())).isEmpty();
    verifyNoInteractions(liveMeetingProvisioningService);
  }

  private java.util.UUID createLiveClassItem(
      java.util.UUID adminId, java.util.UUID courseId, java.util.UUID sectionId) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/admin/courses/{courseId}/live-classes", courseId)
                    .with(authentication(adminAuth(adminId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "sectionId":"%s",
                          "title":"External workshop",
                          "expectedDurationMinutes":60
                        }
                        """
                            .formatted(sectionId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return java.util.UUID.fromString(
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(response)
            .get("liveClassItemId")
            .asText());
  }

  private java.util.UUID scheduleExternal(
      java.util.UUID adminId, java.util.UUID courseId, java.util.UUID itemId) throws Exception {
    String response =
        mockMvc
            .perform(
                post(
                        "/admin/courses/{courseId}/live-classes/{liveClassId}/schedule-with-url",
                        courseId,
                        itemId)
                    .with(authentication(adminAuth(adminId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "startsAt":"2099-11-01T14:00:00Z",
                          "endsAt":"2099-11-01T15:00:00Z",
                          "provider":"OTHER",
                          "participantJoinUrl":"https://class.example.com/room/abc",
                          "maxCapacity":50
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return java.util.UUID.fromString(
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(response)
            .get("liveClassId")
            .asText());
  }

  @Test
  void repeatCourseClonesCurriculumIndependentlyOfSource() throws Exception {
    var admin = user("Repeat Admin", "repeat-admin@example.com");
    var source = course("Repeatable Course", "repeatable-spring", admin);
    var sourceSection = section(source, 1);
    var sourceLesson = lesson(source, sourceSection, 1);
    sourceSection.setReleaseType(ReleaseType.FIXED_DATE);
    sourceSection.setReleaseAt(Instant.parse("2030-01-01T00:00:00Z"));
    courseSectionRepository.saveAndFlush(sourceSection);
    sourceLesson.setReleaseType(ReleaseType.FIXED_DATE);
    sourceLesson.setReleaseAt(Instant.parse("2030-01-02T00:00:00Z"));
    lessonRepository.saveAndFlush(sourceLesson);
    var sourceMedia = mediaAsset(sourceLesson, "repeat-shared-playback");
    var sourceQuiz = quiz(source, "Versioned quiz");
    var sourceLiveClass = liveClass(source, sourceSection, sourceLesson);

    String response =
        mockMvc
            .perform(
                post("/admin/courses/{courseId}/repeat", source.getId())
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "slug":"repeatable-fall",
                          "priceBdt":1800,
                          "studyMode":"COHORT_BASED",
                          "timezone":"America/Chicago",
                          "startsAt":"2030-09-01T14:00:00Z",
                          "endsAt":"2030-12-15T14:00:00Z",
                          "capacity":40
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.courseId").value(org.hamcrest.Matchers.not(source.getId().toString())))
            .andExpect(jsonPath("$.slug").value("repeatable-fall"))
            .andExpect(jsonPath("$.title").value("Repeatable Course"))
            .andExpect(jsonPath("$.studyMode").value("COHORT_BASED"))
            .andExpect(jsonPath("$.timezone").value("America/Chicago"))
            .andExpect(jsonPath("$.capacity").value(40))
            .andExpect(jsonPath("$.sections[0].title").value(sourceSection.getTitle()))
            .andReturn()
            .getResponse()
            .getContentAsString();

    java.util.UUID repeatedId =
        java.util.UUID.fromString(
            new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response)
                .get("courseId")
                .asText());
    var repeatedSection =
        courseSectionRepository.findByCourseIdOrderByPositionAsc(repeatedId).getFirst();
    assertThat(courseSectionRepository.findByCourseIdOrderByPositionAsc(repeatedId))
        .hasSize(1)
        .extracting("id")
        .doesNotContain(sourceSection.getId());
    var repeatedLesson = lessonRepository.findByCourseIdOrderByPositionAsc(repeatedId).getFirst();
    assertThat(lessonRepository.findByCourseIdOrderByPositionAsc(repeatedId))
        .hasSize(1)
        .extracting("id")
        .doesNotContain(sourceLesson.getId());
    assertThat(repeatedSection.getReleaseType()).isEqualTo(ReleaseType.IMMEDIATE);
    assertThat(repeatedSection.getReleaseAt()).isNull();
    assertThat(repeatedLesson.getReleaseType()).isEqualTo(ReleaseType.IMMEDIATE);
    assertThat(repeatedLesson.getReleaseAt()).isNull();
    assertThat(mediaAssetRepository.findByLessonId(repeatedLesson.getId()).orElseThrow())
        .satisfies(
            media -> {
              assertThat(media.getProviderAssetId()).isEqualTo(sourceMedia.getProviderAssetId());
              assertThat(media.getPlaybackId()).isEqualTo(sourceMedia.getPlaybackId());
            });
    assertThat(
            quizRepository.findBySectionIdOrderByPositionAsc(
                courseSectionRepository
                    .findByCourseIdOrderByPositionAsc(repeatedId)
                    .getFirst()
                    .getId()))
        .hasSize(1)
        .extracting("id")
        .doesNotContain(sourceQuiz.getId());
    assertThat(liveClassRepository.findByCourseIdOrderByStartsAtAsc(repeatedId)).isEmpty();
    assertThat(liveClassRepository.findByCourseIdOrderByStartsAtAsc(source.getId()))
        .extracting("id")
        .containsExactly(sourceLiveClass.getId());

    mockMvc
        .perform(
            patch("/admin/courses/{courseId}", repeatedId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"priceBdt\":1900}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            patch("/admin/courses/{courseId}", repeatedId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Must Not Leak\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/admin/courses/{courseId}/sections", repeatedId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Shared Mutation\",\"slug\":\"shared-mutation\",\"position\":2}"))
        .andExpect(status().isOk());
    assertThat(courseRepository.findById(source.getId()).orElseThrow().getTitle())
        .isEqualTo("Repeatable Course");
  }

  @Test
  void publishedCurriculumRemainsDirectlyEditableEvenWithActiveEnrollments() throws Exception {
    var admin = user("Mutable Admin", "mutable-curriculum-admin@example.com");
    final var student = user("Mutable Student", "mutable-curriculum-student@example.com");
    var course = course("Mutable Curriculum", "mutable-curriculum", admin);
    var section = section(course, 1);
    var mutableLesson = lesson(course, section, 1);
    var scheduledClass = liveClass(course, section, mutableLesson);

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/publish", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Edited after publish\"}"))
        .andExpect(status().isOk());
    var editedCourse = courseRepository.findById(course.getId()).orElseThrow();
    assertThat(editedCourse.getId()).isEqualTo(course.getId());
    assertThat(editedCourse.getTitle()).isEqualTo("Edited after publish");
    assertThat(editedCourse.getStatus()).isEqualTo(com.gii.common.enums.PublishStatus.PUBLISHED);
    assertThat(liveClassRepository.findById(scheduledClass.getId())).isPresent();

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/sections", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"New Section\",\"slug\":\"new-section\",\"position\":2}"))
        .andExpect(status().isOk());

    enrollment(student, editedCourse, com.gii.common.enums.EnrollmentStatus.ACTIVE);
    mockMvc
        .perform(
            patch("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Edited with active enrollment\"}"))
        .andExpect(status().isOk());
    assertThat(courseRepository.findById(course.getId()).orElseThrow().getTitle())
        .isEqualTo("Edited with active enrollment");
  }

  @Autowired private MockMvc mockMvc;

  @AfterEach
  void cleanup() {
    cleanupAdminData();
  }

  @Test
  void createGetUpdateAndPublishCourseShouldPersistState() throws Exception {
    var admin = user("Admin One", "admin-course@example.com");
    var category = category("প্রোগ্রামিং", "Programming", "programming");

    mockMvc
        .perform(
            post("/admin/courses")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"Course Alpha",
                      "slug":"course-alpha",
                      "categoryIds":["%s"],
                      "priceBdt":1500,
                      "level":"BEGINNER",
                      "language":"EN",
                      "studyMode":"COHORT_BASED",
                      "isFree":false
                    }
                    """
                        .formatted(category.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Course Alpha"))
        .andExpect(jsonPath("$.categories[0].id").value(category.getId().toString()))
        .andExpect(jsonPath("$.status").value("DRAFT"));

    var course =
        courseRepository.findAll().stream()
            .filter(c -> "course-alpha".equals(c.getSlug()))
            .findFirst()
            .orElseThrow();
    mockMvc
        .perform(
            get("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courseId").value(course.getId().toString()));

    mockMvc
        .perform(
            patch("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Course Alpha Updated\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Course Alpha Updated"));

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/sections", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Section Publish\",\"slug\":\"section-publish\",\"position\":1}"))
        .andExpect(status().isOk());

    var section =
        courseSectionRepository.findByCourseIdOrderByPositionAsc(course.getId()).getFirst();
    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/lessons", section.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Lesson Publish","slug":"lesson-publish","position":1,"lessonType":"VIDEO"}
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/publish", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());

    var updated = courseRepository.findById(course.getId()).orElseThrow();
    org.assertj.core.api.Assertions.assertThat(updated.getStatus())
        .isEqualTo(PublishStatus.PUBLISHED);
    org.assertj.core.api.Assertions.assertThat(updated.getPublishedAt()).isNotNull();
  }

  @Test
  void sectionAndLessonLifecycleShouldPersist() throws Exception {
    var admin = user("Admin Two", "admin-structure@example.com");
    var creator = user("Creator Two", "creator-structure@example.com");
    var course = course("Course Struct", "course-struct", creator);

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/sections", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Section A\",\"slug\":\"section-a\",\"position\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Section A"));

    var section =
        courseSectionRepository.findByCourseIdOrderByPositionAsc(course.getId()).getFirst();

    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/lessons", section.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Lesson A","slug":"lesson-a","position":1,"lessonType":"VIDEO"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Lesson A"));

    var lesson = lessonRepository.findByCourseIdOrderByPositionAsc(course.getId()).getFirst();
    mockMvc
        .perform(
            post("/admin/courses/{courseId}/structure/reorder", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sections":[
                        {
                          "sectionId":"%s",
                          "newPosition":2,
                          "items":[{"itemId":"%s","itemType":"LESSON","newPosition":3}]
                        }
                      ]
                    }
                    """
                        .formatted(section.getId(), lesson.getId())))
        .andExpect(status().isOk());

    org.assertj.core.api.Assertions.assertThat(
            courseSectionRepository.findById(section.getId()).orElseThrow().getPosition())
        .isEqualTo(2);
    org.assertj.core.api.Assertions.assertThat(
            sectionItemRepository
                .findByItemTypeAndItemId(
                    com.gii.common.enums.SectionItemType.LESSON, lesson.getId())
                .orElseThrow()
                .getPosition())
        .isEqualTo(3);
  }

  @Test
  void sectionItemsShouldReturnMixedOrderedLessonsQuizzesAndLiveClasses() throws Exception {
    var admin = user("Admin Three", "admin-items@example.com");
    var creator = user("Creator Three", "creator-items@example.com");
    var course = course("Course Items", "course-items", creator);
    var section = section(course, 1);
    var lesson = lesson(course, section, 1);

    mockMvc
        .perform(
            post("/admin/sections/{sectionId}/quizzes", section.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId":"%s",
                      "position":2,
                      "title":"Quiz Mixed",
                      "passingScorePct":60,
                      "maxAttempts":3,
                      "timeLimitSec":600,
                      "questions":[
                        {"position":1,"questionText":"Q1","questionType":"MCQ","points":1,"choices":[
                          {"choiceText":"A","isCorrect":true}
                        ]}
                      ]
                    }
                    """
                        .formatted(section.getId())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/live-classes/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId":"%s",
                      "position":3,
                      "title":"Live Workshop",
                      "startsAt":"%s",
                      "endsAt":"%s",
                      "provider":"ZOOM",
                      "maxCapacity":100
                    }
                    """
                        .formatted(
                            section.getId(),
                            java.time.Instant.now().plusSeconds(3600),
                            java.time.Instant.now().plusSeconds(7200))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.position").value(3));

    mockMvc
        .perform(
            get("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].items[0].itemType").value("LESSON"))
        .andExpect(jsonPath("$.sections[0].items[0].position").value(1))
        .andExpect(jsonPath("$.sections[0].items[1].itemType").value("QUIZ"))
        .andExpect(jsonPath("$.sections[0].items[1].position").value(2))
        .andExpect(jsonPath("$.sections[0].items[2].itemType").value("LIVE_CLASS"))
        .andExpect(jsonPath("$.sections[0].items[2].position").value(3))
        .andExpect(jsonPath("$.sections[0].items[0].lesson.title").value(lesson.getTitle()))
        .andExpect(jsonPath("$.sections[0].items[1].quiz.title").value("Quiz Mixed"))
        .andExpect(jsonPath("$.sections[0].items[2].liveClass.title").value("Live Workshop"));
  }

  @Test
  void reorderStructureShouldValidateDuplicateItemPositions() throws Exception {
    var admin = user("Admin Five", "admin-reorder-validation@example.com");
    var creator = user("Creator Five", "creator-reorder-validation@example.com");
    var course = course("Course Reorder", "course-reorder", creator);
    var section = section(course, 1);
    var lesson = lesson(course, section, 1);
    var quiz = quiz(course, "Quiz Reorder");

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/structure/reorder", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sections":[
                        {
                          "sectionId":"%s",
                          "newPosition":1,
                          "items":[
                            {"itemId":"%s","itemType":"LESSON","newPosition":1},
                            {"itemId":"%s","itemType":"QUIZ","newPosition":1}
                          ]
                        }
                      ]
                    }
                    """
                        .formatted(section.getId(), lesson.getId(), quiz.getId())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reorderStructureShouldUpdateMixedItemPositions() throws Exception {
    var admin = user("Admin Six", "admin-reorder-mixed@example.com");
    var creator = user("Creator Six", "creator-reorder-mixed@example.com");
    var course = course("Course Reorder Mixed", "course-reorder-mixed", creator);
    var section = section(course, 1);
    var lesson = lesson(course, section, 1);
    var quiz = quiz(course, "Quiz Mixed Reorder");
    var liveClass = liveClass(course, section, lesson);

    mockMvc
        .perform(
            post("/admin/courses/{courseId}/structure/reorder", course.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sections":[
                        {
                          "sectionId":"%s",
                          "newPosition":1,
                          "items":[
                            {"itemId":"%s","itemType":"LESSON","newPosition":3},
                            {"itemId":"%s","itemType":"QUIZ","newPosition":2},
                            {"itemId":"%s","itemType":"LIVE_CLASS","newPosition":1}
                          ]
                        }
                      ]
                    }
                    """
                        .formatted(
                            section.getId(), lesson.getId(), quiz.getId(), liveClass.getId())))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/admin/courses/{courseId}", course.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].items[0].itemType").value("LIVE_CLASS"))
        .andExpect(jsonPath("$.sections[0].items[0].position").value(1))
        .andExpect(jsonPath("$.sections[0].items[1].itemType").value("QUIZ"))
        .andExpect(jsonPath("$.sections[0].items[1].position").value(2))
        .andExpect(jsonPath("$.sections[0].items[2].itemType").value("LESSON"))
        .andExpect(jsonPath("$.sections[0].items[2].position").value(3));
  }
}
