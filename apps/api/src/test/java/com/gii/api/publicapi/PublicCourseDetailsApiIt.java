package com.gii.api.publicapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.SectionItem;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassSlot;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizQuestion;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.LessonResourcePurpose;
import com.gii.common.enums.LiveClassProvider;
import com.gii.common.enums.LiveClassStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.QuestionType;
import com.gii.common.enums.SectionItemType;
import com.gii.common.enums.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class PublicCourseDetailsApiIt extends AbstractPublicApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsOnlyPublishedCourseDetailsAndPublishedContent() throws Exception {
    User creator = user("Creator", "creator4@example.com", UserStatus.ACTIVE);
    Course published =
        course(
            "Public Course",
            uniqueSlug("public-course"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    published.setYoutubeVideoId("dQw4w9WgXcQ");
    courseRepository.save(published);
    CourseSection publishedSection =
        section(published, uniqueSlug("sec-p"), 1, PublishStatus.PUBLISHED);
    section(published, uniqueSlug("sec-d"), 2, PublishStatus.DRAFT);
    Lesson freeLesson =
        lesson(
            published,
            publishedSection,
            uniqueSlug("lesson-free"),
            1,
            PublishStatus.PUBLISHED,
            true);
    lesson(
        published, publishedSection, uniqueSlug("lesson-paid"), 4, PublishStatus.PUBLISHED, false);
    lesson(published, publishedSection, uniqueSlug("lesson-draft"), 6, PublishStatus.DRAFT, true);
    mediaAsset(freeLesson, "yt123");
    lessonResource(freeLesson, "Course handbook", LessonResourcePurpose.PRIMARY_CONTENT, 1);
    lessonResource(freeLesson, "Exercise sheet", LessonResourcePurpose.SUPPLEMENTARY, 2);

    Quiz quiz =
        quizRepository.save(
            Quiz.builder()
                .section(publishedSection)
                .position(2)
                .title("Knowledge check")
                .passingScorePct(70)
                .maxAttempts(2)
                .timeLimitSec(600)
                .status(PublishStatus.PUBLISHED)
                .build());
    sectionItemRepository.save(
        SectionItem.builder()
            .section(publishedSection)
            .itemType(SectionItemType.QUIZ)
            .itemId(quiz.getId())
            .position(2)
            .build());
    quizQuestionRepository.save(
        QuizQuestion.builder()
            .quiz(quiz)
            .position(1)
            .questionText("A buyer must not see this question")
            .questionType(QuestionType.MCQ)
            .points(1)
            .build());

    Quiz draftQuiz =
        quizRepository.save(
            Quiz.builder()
                .section(publishedSection)
                .position(7)
                .title("Draft quiz")
                .status(PublishStatus.DRAFT)
                .build());
    sectionItemRepository.save(
        SectionItem.builder()
            .section(publishedSection)
            .itemType(SectionItemType.QUIZ)
            .itemId(draftQuiz.getId())
            .position(7)
            .build());

    LiveClassSlot scheduledSlot =
        liveClassSlotRepository.save(
            LiveClassSlot.builder()
                .section(publishedSection)
                .title("Live workshop")
                .description("Work through examples together")
                .expectedDurationMinutes(60)
                .isMandatory(true)
                .build());
    sectionItemRepository.save(
        SectionItem.builder()
            .section(publishedSection)
            .itemType(SectionItemType.LIVE_CLASS)
            .itemId(scheduledSlot.getId())
            .position(3)
            .build());
    Instant liveStartsAt = Instant.parse("2030-05-01T15:00:00Z");
    liveClassRepository.save(
        LiveClass.builder()
            .course(published)
            .slot(scheduledSlot)
            .provider(LiveClassProvider.ZOOM)
            .providerMeetingId("private-meeting-id")
            .hostStartUrl("https://private.example/host")
            .participantJoinUrl("https://private.example/join")
            .startsAt(liveStartsAt)
            .endsAt(liveStartsAt.plusSeconds(3600))
            .status(LiveClassStatus.SCHEDULED)
            .build());

    LiveClassSlot unscheduledSlot =
        liveClassSlotRepository.save(
            LiveClassSlot.builder()
                .section(publishedSection)
                .title("Office hours")
                .expectedDurationMinutes(30)
                .isMandatory(false)
                .build());
    sectionItemRepository.save(
        SectionItem.builder()
            .section(publishedSection)
            .itemType(SectionItemType.LIVE_CLASS)
            .itemId(unscheduledSlot.getId())
            .position(5)
            .build());

    mockMvc
        .perform(get("/public/courses/{slug}", published.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Public Course"))
        .andExpect(jsonPath("$.video.provider").value("YOUTUBE"))
        .andExpect(jsonPath("$.video.sourceId").value("dQw4w9WgXcQ"))
        .andExpect(jsonPath("$.sections.length()").value(1))
        .andExpect(jsonPath("$.sections[0].lessons.length()").value(2))
        .andExpect(jsonPath("$.sections[0].items.length()").value(5))
        .andExpect(jsonPath("$.sections[0].items[0].itemType").value("LESSON"))
        .andExpect(jsonPath("$.sections[0].items[0].position").value(1))
        .andExpect(jsonPath("$.sections[0].items[0].lesson.video.sourceId").value("yt123"))
        .andExpect(jsonPath("$.sections[0].items[1].itemType").value("QUIZ"))
        .andExpect(jsonPath("$.sections[0].items[1].quiz.title").value("Knowledge check"))
        .andExpect(jsonPath("$.sections[0].items[1].quiz.questionCount").value(1))
        .andExpect(jsonPath("$.sections[0].items[1].quiz.questions").doesNotExist())
        .andExpect(jsonPath("$.sections[0].items[2].itemType").value("LIVE_CLASS"))
        .andExpect(jsonPath("$.sections[0].items[2].liveClass.title").value("Live workshop"))
        .andExpect(jsonPath("$.sections[0].items[2].liveClass.scheduled").value(true))
        .andExpect(
            jsonPath("$.sections[0].items[2].liveClass.startsAt")
                .value(liveStartsAt.toString()))
        .andExpect(jsonPath("$.sections[0].items[2].liveClass.providerMeetingId").doesNotExist())
        .andExpect(jsonPath("$.sections[0].items[2].liveClass.hostStartUrl").doesNotExist())
        .andExpect(jsonPath("$.sections[0].items[2].liveClass.participantJoinUrl").doesNotExist())
        .andExpect(jsonPath("$.sections[0].items[3].itemType").value("LESSON"))
        .andExpect(jsonPath("$.sections[0].items[4].itemType").value("LIVE_CLASS"))
        .andExpect(jsonPath("$.sections[0].items[4].liveClass.title").value("Office hours"))
        .andExpect(jsonPath("$.sections[0].items[4].liveClass.scheduled").value(false))
        .andExpect(jsonPath("$.sections[0].lessons[0].video.sourceId").value("yt123"))
        .andExpect(
            jsonPath("$.sections[0].lessons[0].primaryResource.title").value("Course handbook"))
        .andExpect(jsonPath("$.sections[0].lessons[0].primaryResource.resourceType").value("PDF"))
        .andExpect(jsonPath("$.sections[0].lessons[0].resources.length()").value(1))
        .andExpect(jsonPath("$.sections[0].lessons[0].resources[0].title").value("Exercise sheet"))
        .andExpect(jsonPath("$.sections[0].lessons[0].resources[0].downloadUrl").doesNotExist())
        .andExpect(jsonPath("$.sections[0].lessons[1].video").doesNotExist());
  }

  @Test
  void returns404ForUnknownOrUnpublishedCourse() throws Exception {
    User creator = user("Creator", "creator5@example.com", UserStatus.ACTIVE);
    Course draft =
        course(
            "Draft Course",
            uniqueSlug("draft-details"),
            PublishStatus.DRAFT,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            null);

    mockMvc.perform(get("/public/courses/{slug}", "missing-slug")).andExpect(status().isNotFound());
    mockMvc
        .perform(get("/public/courses/{slug}", draft.getSlug()))
        .andExpect(status().isNotFound());
  }

  @Test
  void includesCategoriesAndInstructors() throws Exception {
    User creator = user("Creator", "creator11@example.com", UserStatus.ACTIVE);
    User instructor = user("Instructor X", "ins-x@example.com", UserStatus.ACTIVE);
    Course course =
        course(
            "With Relations",
            uniqueSlug("with-rel"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.EN,
            Instant.now());
    attachCategory(course, category("Programming", uniqueSlug("programming")));
    attachInstructor(course, instructor);

    mockMvc
        .perform(get("/public/courses/{slug}", course.getSlug()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories.length()").value(1))
        .andExpect(jsonPath("$.categories[0].name").value("Programming"))
        .andExpect(jsonPath("$.instructors.length()").value(1))
        .andExpect(jsonPath("$.instructors[0].fullName").value("Instructor X"));
  }

  @Test
  void returnsEnglishCourseAndNestedContentWhenRequested() throws Exception {
    User creator = user("Creator", "creator-localized-course@example.com", UserStatus.ACTIVE);
    Course course =
        course(
            "বাংলা কোর্স",
            uniqueSlug("localized-course"),
            PublishStatus.PUBLISHED,
            creator,
            CourseLevel.BEGINNER,
            CourseLanguage.BN,
            Instant.now());
    course.setTitleEn("English Course");
    course.setShortDescription("বাংলা সংক্ষিপ্ত বিবরণ");
    course.setShortDescriptionEn("English short description");
    course.setDescription("বাংলা বিবরণ");
    course.setDescriptionEn("English description");
    course.setHighlights(java.util.List.of("বাংলা হাইলাইট"));
    course.setHighlightsEn(java.util.List.of("English highlight"));
    courseRepository.saveAndFlush(course);

    var category = category("বাংলা বিভাগ", uniqueSlug("localized-category"));
    category.setNameEn("English Category");
    categoryRepository.saveAndFlush(category);
    attachCategory(course, category);

    CourseSection section =
        section(course, uniqueSlug("localized-section"), 1, PublishStatus.PUBLISHED);
    section.setTitle("বাংলা অধ্যায়");
    section.setTitleEn("English Section");
    courseSectionRepository.saveAndFlush(section);
    Lesson lesson =
        lesson(course, section, uniqueSlug("localized-lesson"), 1, PublishStatus.PUBLISHED, true);
    lesson.setTitle("বাংলা পাঠ");
    lesson.setTitleEn("English Lesson");
    lessonRepository.saveAndFlush(lesson);

    mockMvc
        .perform(get("/public/courses/{slug}", course.getSlug()).param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("English Course"))
        .andExpect(jsonPath("$.shortDescription").value("English short description"))
        .andExpect(jsonPath("$.description").value("English description"))
        .andExpect(jsonPath("$.highlights[0]").value("English highlight"))
        .andExpect(jsonPath("$.categories[0].name").value("English Category"))
        .andExpect(jsonPath("$.sections[0].title").value("English Section"))
        .andExpect(jsonPath("$.sections[0].lessons[0].title").value("English Lesson"));
  }
}
