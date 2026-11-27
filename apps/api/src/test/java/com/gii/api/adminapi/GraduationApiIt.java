package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.api.model.request.admin.CreateWaiverRequest;
import com.gii.api.model.request.admin.ReviewWaiverRequest;
import com.gii.api.model.request.admin.SaveProgramCurriculumRequest;
import com.gii.api.model.request.admin.SetCollectionCoursesRequest;
import com.gii.api.service.academic.AcademicProgressService;
import com.gii.api.service.academic.CourseWaiverService;
import com.gii.api.service.academic.ProgramCurriculumService;
import com.gii.api.service.certificate.CertificateDocumentService;
import com.gii.api.service.certificate.CertificateIssueService;
import com.gii.api.service.progress.CollectionEnrollmentCompletionService;
import com.gii.api.service.student.StudentCollectionDetailsService;
import com.gii.api.service.student.StudentCollectionsService;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.WaiverStatus;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.service.payment.ProgramSnapshotService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class GraduationApiIt extends AbstractAdminApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ProgramCurriculumService curriculum;
  @Autowired ProgramSnapshotService snapshots;
  @Autowired CollectionEnrollmentRepository enrollments;
  @Autowired AcademicAchievementRepository awards;
  @Autowired AcademicProgressService progress;
  @Autowired CollectionEnrollmentCompletionService completion;
  @Autowired CourseWaiverService waivers;
  @Autowired CertificateIssueService certificates;
  @Autowired StudentCollectionsService studentPrograms;
  @Autowired StudentCollectionDetailsService studentProgram;
  @MockitoBean CertificateDocumentService documents;

  private Authentication student(User user) {
    return new UsernamePasswordAuthenticationToken(
        user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }

  private Collection program(User admin, String slug, int required, int cap, Course... courses) {
    final var p = collection(slug, slug, admin, PublishStatus.DRAFT);
    p.setType(CollectionType.DIPLOMA);
    collectionRepository.saveAndFlush(p);
    final var rows = new java.util.ArrayList<SetCollectionCoursesRequest.Item>();
    for (int i = 0; i < courses.length; i++) {
      rows.add(new SetCollectionCoursesRequest.Item(courses[i].getId(), i + 1, i == 0));
    }
    curriculum.save(p.getId(), new SaveProgramCurriculumRequest(required, cap, 12, null, rows));
    p.setStatus(PublishStatus.PUBLISHED);
    return collectionRepository.saveAndFlush(p);
  }

  private CollectionEnrollment join(User student, Collection p) {
    return enrollments.saveAndFlush(
        CollectionEnrollment.builder()
            .user(student)
            .collection(p)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(864000))
            .programSnapshot(
                snapshots.capture(
                    p, collectionCourseRepository.findByCollection_IdOrderByPositionAsc(p.getId())))
            .build());
  }

  private void award(User student, Course c) {
    final var e = enrollment(student, c, EnrollmentStatus.ACTIVE);
    e.setCompletedAt(Instant.now());
    enrollmentRepository.saveAndFlush(e);
    awards.awardCompletion(e.getId());
  }

  private void waive(User student, User admin, Course c) {
    final var w =
        waivers.create(
            student.getId(),
            new CreateWaiverRequest(c.getTemplate().getId(), "Equivalent prior study", null));
    waivers.review(
        w.id(),
        admin.getId(),
        new ReviewWaiverRequest(WaiverStatus.APPROVED, "Evidence verified", 3, null));
  }

  @Test
  void enoughElectiveCreditsCannotHideMissingMandatoryCoursesOrShowFullProgress() throws Exception {
    final var admin = user("Admin", "graduation-core-admin@example.com");
    final var learner = user("Learner", "graduation-core-student@example.com");
    final var core = course("Required core", "graduation-core", admin, PublishStatus.DRAFT);
    final var elective = course("Elective", "graduation-elective", admin, PublishStatus.PUBLISHED);
    final var p = program(admin, "graduation-core-program", 3, 3, core, elective);
    final var e = join(learner, p);
    award(learner, elective);
    mvc.perform(
            get("/student/collections/{id}/graduation", p.getId())
                .with(authentication(student(learner))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.creditsRemaining").value(0))
        .andExpect(jsonPath("$.creditProgress.mandatoryCoursesCompleted").value(0))
        .andExpect(jsonPath("$.canRequestCertificate").value(false));
    assertThat(studentPrograms.execute(student(learner)).getFirst().progressPercentage()).isZero();
    assertThat(studentProgram.execute(p.getId(), student(learner)).progressPercentage()).isZero();
    progress.check(learner.getId(), p.getId());
    assertThat(e.getCompletedAt()).isNull();
  }

  @Test
  void breakdownCapsElectivesAndExcludesUnrelatedAwardsWithoutErasingTranscriptCredits() {
    final var admin = user("Admin", "graduation-cap-admin@example.com");
    final var learner = user("Learner", "graduation-cap-student@example.com");
    final var core = course("Core", "graduation-cap-core", admin, PublishStatus.PUBLISHED);
    final var elective =
        course("Elective", "graduation-cap-elective", admin, PublishStatus.PUBLISHED);
    final var unrelated = course("Other", "graduation-unrelated", admin, PublishStatus.PUBLISHED);
    final var p = program(admin, "graduation-cap-program", 5, 2, core, elective);
    join(learner, p);
    award(learner, core);
    award(learner, elective);
    award(learner, unrelated);
    final var r = progress.get(learner.getId());
    assertThat(r.academicRecord().totalCredits()).isEqualTo(9);
    final var report = r.programs().getFirst();
    assertThat(report.creditProgress().earnedCredits()).isEqualTo(5);
    assertThat(report.courses()).hasSize(2);
    assertThat(
            report.courses().stream()
                .filter(c -> !c.mandatory())
                .findFirst()
                .orElseThrow()
                .countedCredits())
        .isEqualTo(2);
    assertThat(report.canRequestCertificate()).isTrue();
  }

  @Test
  void oneCourseCreditCanBeReusedAcrossProgramsAndCohortsNeverDuplicateIt() {
    final var admin = user("Admin", "graduation-reuse-admin@example.com");
    final var learner = user("Learner", "graduation-reuse-student@example.com");
    final var c = course("Shared", "graduation-reuse-course", admin, PublishStatus.PUBLISHED);
    final var a = program(admin, "graduation-reuse-a", 3, 0, c);
    final var b = program(admin, "graduation-reuse-b", 3, 0, c);
    join(learner, a);
    join(learner, b);
    award(learner, c);
    final var cohort =
        courseRepository.saveAndFlush(
            Course.builder()
                .template(c.getTemplate())
                .slug("graduation-another-cohort")
                .createdBy(admin)
                .build());
    award(learner, cohort);
    final var r = progress.get(learner.getId());
    assertThat(r.academicRecord().totalCredits()).isEqualTo(3);
    assertThat(r.programs()).allSatisfy(p -> assertThat(p.requirementsMet()).isTrue());
  }

  @Test
  void originalRequirementsAndRecordedCompletionSurviveLaterCurriculumEdits() throws Exception {
    final var admin = user("Admin", "graduation-stable-admin@example.com");
    final var learner = user("Learner", "graduation-stable-student@example.com");
    final var core = course("Core", "graduation-stable-core", admin, PublishStatus.PUBLISHED);
    final var p = program(admin, "graduation-stable-program", 3, 0, core);
    final var e = join(learner, p);
    award(learner, core);
    mvc.perform(
            post("/admin/students/{u}/programs/{p}/graduation/check", learner.getId(), p.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    final var recorded = e.getCompletedAt();
    final var extra = course("Later", "graduation-stable-extra", admin, PublishStatus.DRAFT);
    curriculum.save(
        p.getId(),
        new SaveProgramCurriculumRequest(
            6,
            0,
            24,
            60,
            List.of(
                new SetCollectionCoursesRequest.Item(core.getId(), 1, true),
                new SetCollectionCoursesRequest.Item(extra.getId(), 2, true))));
    final var r = progress.check(learner.getId(), p.getId());
    assertThat(r.completedAt()).isEqualTo(recorded);
    assertThat(r.creditProgress().requiredCredits()).isEqualTo(3);
    assertThat(r.creditProgress().mandatoryCoursesRequired()).isEqualTo(1);
    assertThat(r.courses())
        .filteredOn(c -> c.courseTemplateId().equals(extra.getTemplate().getId()))
        .allSatisfy(c -> assertThat(c.mandatory()).isFalse());
  }

  @Test
  void approvedWaiversAllowProgramGraduationAndCertificateWithoutFakingCourseCompletion()
      throws Exception {
    final var admin = user("Admin", "graduation-waiver-admin@example.com");
    final var learner = user("Learner", "graduation-waiver-student@example.com");
    final var core = course("Core", "graduation-waiver-core", admin, PublishStatus.PUBLISHED);
    final var p = program(admin, "graduation-waiver-program", 3, 0, core);
    final var e = join(learner, p);
    waive(learner, admin, core);
    assertThat(e.getCompletedAt()).isNotNull();
    assertThat(enrollmentRepository.findByUserIdAndCourseId(learner.getId(), core.getId()))
        .isEmpty();
    assertThat(progress.getProgram(learner.getId(), p.getId()).courses().getFirst().source().name())
        .isEqualTo("EXEMPTION");
    when(documents.generateAndUpload(any(), any(), any()))
        .thenReturn(
            new CertificateDocumentService.StoredCertificateDocument(
                null, "test/program.pdf", 100));
    mvc.perform(
            post("/student/collections/{p}/certificate", p.getId())
                .with(authentication(student(learner))))
        .andExpect(status().isOk());
    final var r = progress.getProgram(learner.getId(), p.getId());
    assertThat(r.certificateId()).isNotNull();
    e.setExpiresAt(Instant.now().minusSeconds(10));
    enrollments.saveAndFlush(e);
    assertThat(progress.getProgram(learner.getId(), p.getId()).canRequestCertificate()).isTrue();
    mvc.perform(
            post("/student/collections/{p}/certificate", p.getId())
                .with(authentication(student(learner))))
        .andExpect(status().isOk());
    final var issued = certificateRepository.findById(r.certificateId()).orElseThrow();
    issued.setRevokedAt(Instant.now());
    certificateRepository.saveAndFlush(issued);
    assertThat(progress.getProgram(learner.getId(), p.getId()).canRequestCertificate()).isFalse();
    mvc.perform(
            post("/student/collections/{p}/certificate", p.getId())
                .with(authentication(student(learner))))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/student/courses/{c}/certificate", core.getId())
                .with(authentication(student(learner))))
        .andExpect(status().isForbidden());
  }

  @Test
  void inactiveAccessPreventsNewGraduationAndCertificateBackfillButRetainsCredits()
      throws Exception {
    final var admin = user("Admin", "graduation-expired-admin@example.com");
    final var learner = user("Learner", "graduation-expired-student@example.com");
    final var core = course("Core", "graduation-expired-core", admin, PublishStatus.PUBLISHED);
    final var p = program(admin, "graduation-expired-program", 3, 0, core);
    final var e = join(learner, p);
    e.setExpiresAt(Instant.now().minusSeconds(30));
    enrollments.saveAndFlush(e);
    waive(learner, admin, core);
    assertThat(e.getCompletedAt()).isNull();
    mvc.perform(
            post("/student/collections/{p}/graduation/check", p.getId())
                .with(authentication(student(learner))))
        .andExpect(status().isForbidden());
    final var r = progress.getProgram(learner.getId(), p.getId());
    assertThat(r.requirementsMet()).isTrue();
    assertThat(r.canRequestCertificate()).isFalse();
    assertThat(progress.get(learner.getId()).academicRecord().totalCredits()).isEqualTo(3);
    e.setCompletedAt(Instant.now().minusSeconds(60));
    enrollments.saveAndFlush(e);
    // Exercise the backfill method against this test's transaction-visible fixture.
    AopTestUtils.<CertificateIssueService>getTargetObject(certificates)
        .tryIssueCollectionCertificate(e.getId());
    assertThat(certificateRepository.findByUserIdAndCollectionId(learner.getId(), p.getId()))
        .isEmpty();
    assertThat(progress.check(learner.getId(), p.getId()).completedAt())
        .isEqualTo(e.getCompletedAt());
    e.setStatus(EnrollmentStatus.REFUNDED);
    e.setCompletedAt(null);
    enrollments.saveAndFlush(e);
    completion.refresh(learner.getId(), p.getId());
    assertThat(e.getCompletedAt()).isNull();
  }

  @Test
  void transcriptIncludesSemesterAndEndpointsEnforceOwnershipAndAdminRoles() throws Exception {
    final var admin = user("Admin", "graduation-auth-admin@example.com");
    final var learner = user("Learner", "graduation-auth-student@example.com");
    final var other = user("Other", "graduation-auth-other@example.com");
    final var core = course("Core", "graduation-auth-core", admin, PublishStatus.PUBLISHED);
    core.setSemester(academicSemester());
    courseRepository.saveAndFlush(core);
    final var p = program(admin, "graduation-auth-program", 3, 0, core);
    join(learner, p);
    award(learner, core);
    mvc.perform(get("/student/academic-progress").with(authentication(student(learner))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.academicRecord.totalCredits").value(3))
        .andExpect(jsonPath("$.semesters[0].name").value("Test academic semester"));
    mvc.perform(
            get("/student/collections/{p}/graduation", p.getId())
                .with(authentication(student(other))))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/student/collections/{p}/graduation/check", p.getId())
                .with(authentication(student(other))))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/admin/students/{u}/academic-progress", learner.getId())
                .with(authentication(student(other))))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/admin/students/{u}/programs/{p}/graduation/check", learner.getId(), p.getId())
                .with(authentication(student(other))))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/admin/students/{u}/academic-progress", learner.getId())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    mvc.perform(
            get("/admin/students/{u}/academic-progress", UUID.randomUUID())
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isNotFound());
  }

  @Test
  void legacyProgramsStillRequireOriginalMandatoryCompletion() {
    final var admin = user("Admin", "graduation-legacy-admin@example.com");
    final var learner = user("Learner", "graduation-legacy-student@example.com");
    final var core = course("Core", "graduation-legacy-core", admin, PublishStatus.PUBLISHED);
    final var p = collection("Legacy", "graduation-legacy-program", admin, PublishStatus.PUBLISHED);
    collectionCourse(p, core, 1, true);
    join(learner, p);
    assertThat(progress.getProgram(learner.getId(), p.getId()).creditProgress()).isNull();
    assertThat(progress.check(learner.getId(), p.getId()).completedAt()).isNull();
    award(learner, core);
    assertThat(progress.check(learner.getId(), p.getId()).completedAt()).isNotNull();
  }
}
