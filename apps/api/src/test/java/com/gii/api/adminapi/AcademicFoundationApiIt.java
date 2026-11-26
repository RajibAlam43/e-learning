package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.service.academic.AcademicAchievementService;
import com.gii.api.service.progress.EnrollmentCompletionService;
import com.gii.common.entity.academic.Semester;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.enrollment.LessonProgress;
import com.gii.common.entity.user.User;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.academic.SemesterRepository;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Transactional
class AcademicFoundationApiIt extends AbstractAdminApiIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private PlatformTransactionManager transactions;
  @Autowired private SemesterRepository semesters;
  @Autowired private AcademicAchievementRepository achievements;
  @Autowired private AcademicAchievementService achievementService;
  @Autowired private EnrollmentCompletionService completion;
  @Autowired private LessonProgressRepository progress;
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void semestersValidateDatesAndCodesAndProtectAdminEndpoints() throws Exception {
    User admin = user("Academic Admin", "academic-admin@example.com");
    String body = semesterBody("SPRING-2027");
    String saved =
        mvc.perform(
                post("/admin/semesters")
                    .with(authentication(adminAuth(admin.getId())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Spring 2027"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = json.readTree(saved).path("semesterId").asText();
    mvc.perform(
            post("/admin/semesters")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/admin/semesters/" + id)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("2027-06-30T18:00:00Z", "2026-06-30T18:00:00Z")))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/admin/semesters").with(authentication(studentAuth(admin.getId()))))
        .andExpect(status().isForbidden());
    mvc.perform(get("/admin/semesters")).andExpect(status().isForbidden());
  }

  @Test
  void newCohortsRequireSemesterAndReuseTheCurriculumWhileSelfPacedRemainsIndependent()
      throws Exception {
    User admin = user("Course Admin", "academic-course-admin@example.com");
    var category = category("Academic", "Academic", "academic");
    Semester semester = semester("COHORT-2027");
    String body = courseBody(category.getId(), semester.getId());
    JsonNode first =
        json.readTree(
            mvc.perform(
                    post("/admin/courses")
                        .with(authentication(adminAuth(admin.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credits").value(3))
                .andExpect(jsonPath("$.semester.semesterId").value(semester.getId().toString()))
                .andExpect(jsonPath("$.startsAt").value("2027-01-01T00:00:00Z"))
                .andReturn()
                .getResponse()
                .getContentAsString());
    UUID firstId = UUID.fromString(first.path("courseId").asText());
    mvc.perform(
            patch("/admin/courses/{id}", firstId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"semesterId\":null}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/admin/courses/{id}", firstId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"startsAt\":null,\"endsAt\":null,"
                        + "\"enrollmentStartsAt\":null,\"enrollmentEndsAt\":null}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startsAt").value("2027-01-01T00:00:00Z"));

    var course = courseRepository.findById(firstId).orElseThrow();
    section(course, 1);
    mvc.perform(
            post("/admin/courses/{id}/offerings", firstId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"slug":"academic-spring-b","priceBdt":2000,"studyMode":"COHORT_BASED","semesterId":"%s"}
                    """
                        .formatted(semester.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.courseTemplateId").value(first.path("courseTemplateId").asText()))
        .andExpect(jsonPath("$.sections.length()").value(1));
    mvc.perform(
            post("/admin/courses")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body.replace("academic-spring", "no-semester")
                        .replace(semester.getId().toString(), "")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/admin/courses")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body.replace("academic-spring", "self-paced")
                        .replace("COHORT_BASED", "SELF_PACED")
                        .replace(",\"semesterId\":\"" + semester.getId() + "\"", "")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.semester").isEmpty());
    mvc.perform(
            patch("/admin/courses/{id}", firstId)
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credits\":0}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/admin/semesters/{id}", semester.getId())
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    semesterBody(semester.getCode())
                        .replace("2027-06-30T18:00:00Z", "2027-05-01T00:00:00Z")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void completionAwardsCreditsOncePerTemplateAndSnapshotsCreditsAndSemester() throws Exception {
    User creator = user("Creator", "credit-creator@example.com");
    final User student = user("Student", "credit-student@example.com");
    Course first = course("Credit course", "credit-course-a", creator, PublishStatus.PUBLISHED);
    first.setSemester(semester("CREDITS-2027"));
    first.setCredits(4);
    first.setTitleEn(" ");
    courseRepository.saveAndFlush(first);
    var section = section(first, 1);
    section.setStatus(PublishStatus.PUBLISHED);
    courseSectionRepository.saveAndFlush(section);
    var lesson = lesson(first, section, 1);
    lesson.setLessonType(LessonType.PDF);
    lesson.setIsMandatory(true);
    lesson.setStatus(PublishStatus.PUBLISHED);
    lessonRepository.saveAndFlush(lesson);
    var enrollment = enrollment(student, first, EnrollmentStatus.ACTIVE);
    completion.refresh(student.getId(), first.getId());
    assertThat(achievements.findByUserIdOrderByAchievedAtDesc(student.getId())).isEmpty();
    progress.saveAndFlush(
        LessonProgress.builder()
            .enrollment(enrollment)
            .lesson(lesson)
            .completedAt(Instant.now())
            .build());
    completion.refresh(student.getId(), first.getId());
    completion.refresh(student.getId(), first.getId());
    var earned = achievements.findByUserIdOrderByAchievedAtDesc(student.getId());
    assertThat(earned).hasSize(1);
    assertThat(earned.getFirst().getCredits()).isEqualTo(4);
    assertThat(earned.getFirst().getCourseTitle()).isEqualTo("Credit course");
    assertThat(earned.getFirst().getSemesterId()).isEqualTo(first.getSemester().getId());
    first.setCredits(7);
    courseRepository.saveAndFlush(first);
    Course repeat =
        courseRepository.saveAndFlush(
            Course.builder()
                .template(first.getTemplate())
                .slug("credit-course-b")
                .createdBy(creator)
                .status(PublishStatus.PUBLISHED)
                .build());
    var repeatEnrollment = enrollment(student, repeat, EnrollmentStatus.ACTIVE);
    repeatEnrollment.setCompletedAt(Instant.now());
    enrollmentRepository.saveAndFlush(repeatEnrollment);
    achievementService.awardCompletion(repeatEnrollment);
    assertThat(achievements.findByUserIdOrderByAchievedAtDesc(student.getId())).hasSize(1);
    mvc.perform(get("/student/academic-record").with(authentication(studentAuth(student.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCredits").value(4));
    User other = user("Other", "credit-other@example.com");
    mvc.perform(get("/student/academic-record").with(authentication(studentAuth(other.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCredits").value(0));
    mvc.perform(
            get("/admin/students/{id}/academic-record", student.getId())
                .with(authentication(studentAuth(other.getId()))))
        .andExpect(status().isForbidden());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentRepeatCompletionsCannotAwardTheSameCourseTwice() throws Exception {
    try {
      var enrollments =
          new TransactionTemplate(transactions)
              .execute(transaction -> {
                var creator = user("Race Creator", "race-credit-creator@example.com");
                var student = user("Race Student", "race-credit-student@example.com");
                var first = course("Race credit", "race-credit-a", creator);
                var second =
                    courseRepository.saveAndFlush(
                        Course.builder()
                            .template(first.getTemplate())
                            .slug("race-credit-b")
                            .createdBy(creator)
                            .build());
                var a = enrollment(student, first, EnrollmentStatus.ACTIVE);
                var b = enrollment(student, second, EnrollmentStatus.ACTIVE);
                a.setCompletedAt(Instant.now());
                b.setCompletedAt(Instant.now());
                enrollmentRepository.saveAndFlush(a);
                enrollmentRepository.saveAndFlush(b);
                return List.of(a, b);
              });
      var a = enrollments.get(0);
      var b = enrollments.get(1);
      var start = new CountDownLatch(1);
      try (var executor = Executors.newFixedThreadPool(2)) {
        var one =
            executor.submit(() -> {
              start.await();
              achievementService.awardCompletion(a);
              return true;
            });
        var two =
            executor.submit(() -> {
              start.await();
              achievementService.awardCompletion(b);
              return true;
            });
        start.countDown();
        assertThat(one.get(10, TimeUnit.SECONDS)).isTrue();
        assertThat(two.get(10, TimeUnit.SECONDS)).isTrue();
      }
      assertThat(achievementService.get(a.getUser().getId()).totalCredits()).isEqualTo(3);
      assertThat(achievementService.get(a.getUser().getId()).achievements()).hasSize(1);
    } finally {
      achievements.deleteAll();
      progress.deleteAll();
      cleanupAdminData();
      semesters.deleteAll();
    }
  }

  @Test
  void historicalCompletedEnrollmentsAreNotAutomaticallyBackfilled() {
    var creator = user("Creator", "historic-credit-creator@example.com");
    var student = user("Student", "historic-credit-student@example.com");
    var course = course("Historical", "historical-credit-course", creator);
    var enrollment = enrollment(student, course, EnrollmentStatus.ACTIVE);
    enrollment.setCompletedAt(Instant.parse("2025-01-01T00:00:00Z"));
    enrollmentRepository.saveAndFlush(enrollment);
    completion.refresh(student.getId(), course.getId());
    assertThat(achievements.findByUserIdOrderByAchievedAtDesc(student.getId())).isEmpty();
  }

  @Test
  void openApiDescribesTheAcademicContracts() throws Exception {
    String document =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode schemas = json.readTree(document).path("components").path("schemas");
    assertThat(schemas.path("CreateCourseRequest").path("properties").has("semesterId")).isTrue();
    assertThat(schemas.path("AdminCourseDetailResponse").path("properties").has("credits"))
        .isTrue();
    Path file = Path.of("build/openapi/academic-step-one.json");
    Files.createDirectories(file.getParent());
    Files.writeString(file, document);
  }

  private Semester semester(String code) {
    return semesters.saveAndFlush(
        Semester.builder()
            .code(code)
            .name("Spring 2027")
            .startsAt(Instant.parse("2027-01-01T00:00:00Z"))
            .endsAt(Instant.parse("2027-06-30T18:00:00Z"))
            .registrationStartsAt(Instant.parse("2026-12-01T00:00:00Z"))
            .registrationEndsAt(Instant.parse("2027-01-15T00:00:00Z"))
            .build());
  }

  private String semesterBody(String code) {
    String body =
        """
        {"code":"%s","name":"Spring 2027","startsAt":"2027-01-01T00:00:00Z",
         "endsAt":"2027-06-30T18:00:00Z","registrationStartsAt":"2026-12-01T00:00:00Z",
         "registrationEndsAt":"2027-01-15T00:00:00Z"}
        """
            .formatted(code);
    return body;
  }

  private String courseBody(UUID categoryId, UUID semesterId) {
    String body =
        """
        {"title":"Academic course","slug":"academic-spring","categoryIds":["%s"],
         "priceBdt":2000,"level":"BEGINNER","language":"EN","studyMode":"COHORT_BASED","semesterId":"%s"}
        """
            .formatted(categoryId, semesterId);
    return body;
  }

  private UsernamePasswordAuthenticationToken studentAuth(UUID id) {
    return new UsernamePasswordAuthenticationToken(
        id, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }
}
