package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.model.request.admin.CreateWaiverRequest;
import com.gii.api.model.request.admin.ManualEnrollmentRequest;
import com.gii.api.model.request.admin.ReviewWaiverRequest;
import com.gii.api.model.request.admin.SaveProgramCurriculumRequest;
import com.gii.api.model.request.admin.SetCollectionCoursesRequest;
import com.gii.api.service.academic.CoursePrerequisiteManagementService;
import com.gii.api.service.academic.CourseWaiverService;
import com.gii.api.service.academic.ManualEnrollmentService;
import com.gii.api.service.academic.ProgramCourseRegistrationService;
import com.gii.api.service.academic.ProgramCurriculumService;
import com.gii.common.entity.academic.Semester;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.order.Order;
import com.gii.common.entity.order.OrderItem;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.WaiverStatus;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.academic.ManualEnrollmentAuditRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.service.payment.PaidOrderEnrollmentService;
import com.gii.common.service.payment.RefundedOrderEnrollmentService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Transactional
class ManualEnrollmentApiIt extends AbstractAdminApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ManualEnrollmentService manual;
  @Autowired ManualEnrollmentAuditRepository audits;
  @Autowired CollectionEnrollmentRepository programs;
  @Autowired AcademicAchievementRepository achievements;
  @Autowired CoursePrerequisiteManagementService prerequisites;
  @Autowired CourseWaiverService waivers;
  @Autowired ProgramCurriculumService curriculum;
  @Autowired ProgramCourseRegistrationService registration;
  @Autowired PaidOrderEnrollmentService paid;
  @Autowired RefundedOrderEnrollmentService refunds;
  @Autowired PlatformTransactionManager transactions;
  private final ObjectMapper json =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private String route(User student) {
    return "/admin/students/" + student.getId() + "/manual-enrollments";
  }

  private ManualEnrollmentRequest request(
      UUID courseId,
      UUID programId,
      boolean funded,
      boolean dates,
      boolean capacity,
      boolean unpublished) {
    return new ManualEnrollmentRequest(
        UUID.randomUUID(),
        courseId,
        programId,
        funded,
        dates,
        capacity,
        unpublished,
        "Approved by admissions after review");
  }

  private ResultActions enroll(User admin, User student, ManualEnrollmentRequest r)
      throws Exception {
    return mvc.perform(
        post(route(student))
            .with(authentication(adminAuth(admin.getId())))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(r)));
  }

  private Collection program(User admin, String slug, Course core, Course... electives) {
    final var p = collection(slug, slug, admin, PublishStatus.DRAFT);
    p.setType(CollectionType.DIPLOMA);
    collectionRepository.saveAndFlush(p);
    final var rows = new java.util.ArrayList<SetCollectionCoursesRequest.Item>();
    rows.add(new SetCollectionCoursesRequest.Item(core.getId(), 1, true));
    for (var c : electives) {
      rows.add(new SetCollectionCoursesRequest.Item(c.getId(), rows.size() + 1, false));
    }
    curriculum.save(
        p.getId(),
        new SaveProgramCurriculumRequest(
            electives.length == 0 ? 3 : 6, electives.length == 0 ? 0 : 3, 12, null, rows));
    p.setStatus(PublishStatus.PUBLISHED);
    return collectionRepository.saveAndFlush(p);
  }

  @Test
  void manualGrantHasExpiryAuditAndIdempotentRetryWithoutPaymentOrCredits() throws Exception {
    final var admin = user("Admissions admin", "manual-admin@example.com");
    final var student = user("Learner", "manual-student@example.com");
    final var course = course("Course", "manual-course", admin, PublishStatus.PUBLISHED);
    course.setAccessDurationDays(30);
    courseRepository.saveAndFlush(course);
    final var body = request(course.getId(), null, false, false, false, false);
    enroll(admin, student, body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changed").value(true))
        .andExpect(jsonPath("$.actorName").value("Admissions admin"));
    final var e =
        enrollmentRepository.findByUserIdAndCourseId(student.getId(), course.getId()).orElseThrow();
    final var original = e.getExpiresAt();
    assertThat(original).isEqualTo(e.getEnrolledAt().plus(Duration.ofDays(30)));
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(e.getSourceOrderItem()).isNull();
    assertThat(e.getCompletedAt()).isNull();
    assertThat(achievements.findByUserIdOrderByAchievedAtDesc(student.getId())).isEmpty();
    assertThat(orderRepository.findAll()).isEmpty();
    enroll(admin, student, body).andExpect(status().isOk());
    assertThat(e.getExpiresAt()).isEqualTo(original);
    assertThat(audits.findAll()).hasSize(1);
    mvc.perform(get(route(student)).with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Learner"))
        .andExpect(jsonPath("$.history[0].reason").value(body.reason()));
    final var altered =
        new ManualEnrollmentRequest(
            body.requestId(), course.getId(), null, false, true, false, false, body.reason());
    enroll(admin, student, altered).andExpect(status().isConflict());
  }

  @Test
  void dateAndCapacityExceptionsMustBeExplicitAndAreRecorded() throws Exception {
    final var admin = user("Admin", "manual-dates-admin@example.com");
    final var student = user("Learner", "manual-dates-student@example.com");
    final var course = course("Closed course", "manual-closed", admin, PublishStatus.PUBLISHED);
    final var semester =
        semesterRepository.saveAndFlush(
            Semester.builder()
                .code("MANUAL-CLOSED")
                .name("Closed semester")
                .startsAt(Instant.now().minusSeconds(864000))
                .endsAt(Instant.now().plusSeconds(864000))
                .registrationStartsAt(Instant.now().minusSeconds(172800))
                .registrationEndsAt(Instant.now().minusSeconds(86400))
                .build());
    course.setSemester(semester);
    course.setCapacity(1);
    course.setEnrollmentEndsAt(Instant.now().minusSeconds(3600));
    courseRepository.saveAndFlush(course);
    enrollment(
        user("Seat occupant", "manual-occupied@example.com"), course, EnrollmentStatus.ACTIVE);
    enroll(admin, student, request(course.getId(), null, false, false, false, false))
        .andExpect(status().isConflict());
    enroll(admin, student, request(course.getId(), null, false, true, false, false))
        .andExpect(status().isConflict());
    enroll(admin, student, request(course.getId(), null, false, true, true, false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.overrideDates").value(true))
        .andExpect(jsonPath("$.overrideCapacity").value(true));
    assertThat(
            enrollmentRepository.countAvailableSeatsInUse(
                course.getId(), EnrollmentStatus.ACTIVE, Instant.now()))
        .isEqualTo(2);
    assertThat(audits.findAll()).hasSize(1);
  }

  @Test
  void unpublishedEnrollmentNeedsAnExplicitException() throws Exception {
    final var admin = user("Admin", "manual-draft-admin@example.com");
    final var student = user("Learner", "manual-draft-student@example.com");
    final var course = course("Planned", "manual-draft", admin, PublishStatus.DRAFT);
    enroll(admin, student, request(course.getId(), null, false, true, true, false))
        .andExpect(status().isConflict());
    enroll(admin, student, request(course.getId(), null, false, false, false, true))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowUnpublished").value(true));
  }

  @Test
  void administrativeOverridesNeverBypassPrerequisitesButApprovedWaiversWork() throws Exception {
    final var admin = user("Admin", "manual-prereq-admin@example.com");
    final var student = user("Learner", "manual-prereq-student@example.com");
    final var required = course("Foundation", "manual-foundation", admin, PublishStatus.PUBLISHED);
    final var target = course("Advanced", "manual-advanced", admin, PublishStatus.PUBLISHED);
    prerequisites.save(target.getId(), List.of(required.getTemplate().getId()));
    enroll(admin, student, request(target.getId(), null, false, true, true, true))
        .andExpect(status().isConflict());
    assertThat(enrollmentRepository.findByUserIdAndCourseId(student.getId(), target.getId()))
        .isEmpty();
    final var waiver =
        waivers.create(
            student.getId(),
            new CreateWaiverRequest(
                required.getTemplate().getId(), "Equivalent external qualification", null));
    waivers.review(
        waiver.id(),
        admin.getId(),
        new ReviewWaiverRequest(WaiverStatus.APPROVED, "Evidence checked", 3, null));
    enroll(admin, student, request(target.getId(), null, false, false, false, false))
        .andExpect(status().isOk());
    assertThat(achievements.findByUserIdOrderByAchievedAtDesc(student.getId())).hasSize(1);
  }

  @Test
  void programGrantDefersSeatsAndPreservesOriginalRequirementsOnReactivation() throws Exception {
    final var admin = user("Admin", "manual-program-admin@example.com");
    final var student = user("Learner", "manual-program-student@example.com");
    final var core = course("Core", "manual-program-core", admin, PublishStatus.DRAFT);
    final var p = program(admin, "manual-program", core);
    enroll(admin, student, request(null, p.getId(), false, false, false, false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changed").value(true));
    final var e = programs.findByUserIdAndCollectionId(student.getId(), p.getId()).orElseThrow();
    final var original = e.getProgramSnapshot();
    assertThat(original.accessDurationMonths()).isEqualTo(36);
    assertThat(e.getExpiresAt())
        .isEqualTo(e.getEnrolledAt().atZone(ZoneOffset.UTC).plusMonths(36).toInstant());
    assertThat(enrollmentRepository.findByUserIdAndCourseId(student.getId(), core.getId()))
        .isEmpty();
    final var elective = course("Elective", "manual-program-elective", admin, PublishStatus.DRAFT);
    curriculum.save(
        p.getId(),
        new SaveProgramCurriculumRequest(
            6,
            3,
            24,
            60,
            List.of(
                new SetCollectionCoursesRequest.Item(core.getId(), 1, true),
                new SetCollectionCoursesRequest.Item(elective.getId(), 2, false))));
    final var expiry = e.getExpiresAt();
    enroll(admin, student, request(null, p.getId(), false, false, false, false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changed").value(false));
    assertThat(e.getExpiresAt()).isEqualTo(expiry);
    e.setStatus(EnrollmentStatus.REVOKED);
    e.setExpiresAt(Instant.now().minusSeconds(10));
    programs.saveAndFlush(e);
    enroll(admin, student, request(null, p.getId(), false, false, false, false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changed").value(true));
    assertThat(e.getProgramSnapshot()).isEqualTo(original);
    assertThat(e.getExpiresAt())
        .isEqualTo(e.getEnrolledAt().atZone(ZoneOffset.UTC).plusMonths(36).toInstant());
  }

  @Test
  void manualProgramUsesExplicitDurationAndUnpublishedException() throws Exception {
    final var admin = user("Admin", "manual-duration-admin@example.com");
    final var student = user("Learner", "manual-duration-student@example.com");
    final var core = course("Core", "manual-duration-core", admin, PublishStatus.DRAFT);
    final var p = program(admin, "manual-duration", core);
    p.setStatus(PublishStatus.DRAFT);
    p.setAccessDurationMonths(18);
    collectionRepository.saveAndFlush(p);
    enroll(admin, student, request(null, p.getId(), false, false, false, false))
        .andExpect(status().isConflict());
    enroll(admin, student, request(null, p.getId(), false, false, false, true))
        .andExpect(status().isOk());
    final var e = programs.findByUserIdAndCollectionId(student.getId(), p.getId()).orElseThrow();
    assertThat(e.getProgramSnapshot().accessDurationMonths()).isEqualTo(18);
  }

  @Test
  void programFundedOverridesPreserveElectiveLimitsAndWithdrawalAudit() throws Exception {
    final var admin = user("Admin", "manual-funded-admin@example.com");
    final var student = user("Learner", "manual-funded-student@example.com");
    final var core = course("Core", "manual-funded-core", admin, PublishStatus.PUBLISHED);
    final var a = course("Elective A", "manual-funded-a", admin, PublishStatus.PUBLISHED);
    final var b = course("Elective B", "manual-funded-b", admin, PublishStatus.PUBLISHED);
    final var p = program(admin, "manual-funded-program", core, a, b);
    manual.enroll(
        student.getId(), admin.getId(), request(null, p.getId(), false, false, false, false));
    a.setEnrollmentEndsAt(Instant.now().minusSeconds(30));
    courseRepository.saveAndFlush(a);
    enroll(admin, student, request(a.getId(), p.getId(), true, false, false, false))
        .andExpect(status().isConflict());
    final var granted =
        manual.enroll(
            student.getId(), admin.getId(), request(a.getId(), p.getId(), true, true, true, false));
    assertThat(granted.registrationId()).isNotNull();
    enroll(admin, student, request(b.getId(), p.getId(), true, true, true, false))
        .andExpect(status().isConflict());
    registration.withdraw(
        student.getId(), granted.registrationId(), admin.getId(), "Change cohort");
    enroll(admin, student, request(b.getId(), p.getId(), true, false, false, false))
        .andExpect(status().isOk());
    final var outside = course("Outside", "manual-funded-outside", admin, PublishStatus.PUBLISHED);
    enroll(admin, student, request(outside.getId(), p.getId(), true, true, true, false))
        .andExpect(status().isForbidden());
  }

  @Test
  void completionAndOtherValidAccessAreNeverResetOrShortened() throws Exception {
    final var admin = user("Admin", "manual-preserve-admin@example.com");
    final var student = user("Learner", "manual-preserve-student@example.com");
    final var c = course("Course", "manual-preserve", admin, PublishStatus.PUBLISHED);
    c.setAccessDurationDays(1);
    courseRepository.saveAndFlush(c);
    final var e = enrollment(student, c, EnrollmentStatus.ACTIVE);
    final var expiry = Instant.now().plusSeconds(864000);
    final var completed = Instant.now().minusSeconds(1000);
    e.setExpiresAt(expiry);
    e.setCompletedAt(completed);
    enrollmentRepository.saveAndFlush(e);
    enroll(admin, student, request(c.getId(), null, false, false, false, false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changed").value(false));
    assertThat(e.getExpiresAt()).isEqualTo(expiry);
    assertThat(e.getCompletedAt()).isEqualTo(completed);
    e.setStatus(EnrollmentStatus.REVOKED);
    enrollmentRepository.saveAndFlush(e);
    enroll(admin, student, request(c.getId(), null, false, false, false, false))
        .andExpect(status().isOk());
    assertThat(e.getCompletedAt()).isEqualTo(completed);
    assertThat(achievements.findByUserIdOrderByAchievedAtDesc(student.getId())).isEmpty();
  }

  @Test
  void pendingPaymentAndItsRefundCannotReplaceOrRevokeAnIndependentManualGrant() {
    final var admin = user("Admin", "manual-refund-admin@example.com");
    final var student = user("Learner", "manual-refund-student@example.com");
    final var c = course("Course", "manual-refund", admin, PublishStatus.PUBLISHED);
    c.setAccessDurationDays(30);
    courseRepository.saveAndFlush(c);
    final var order =
        orderRepository.saveAndFlush(
            Order.builder()
                .user(student)
                .amountBdt(BigDecimal.valueOf(1500))
                .provider(OrderProvider.SSLCOMMERZ)
                .build());
    orderItemRepository.saveAndFlush(
        OrderItem.builder()
            .order(order)
            .course(c)
            .itemType(OrderItemType.COURSE)
            .titleSnapshot("Course")
            .priceBdt(BigDecimal.valueOf(1500))
            .build());
    final var result =
        manual.enroll(
            student.getId(), admin.getId(), request(c.getId(), null, false, false, false, false));
    order.setStatus(OrderStatus.PAID);
    order.setPaidAt(Instant.now());
    orderRepository.saveAndFlush(order);
    paid.grant(order.getId());
    final var e = enrollmentRepository.findById(result.enrollmentId()).orElseThrow();
    assertThat(e.getSourceOrderItem()).isNull();
    order.setStatus(OrderStatus.REFUNDED);
    orderRepository.saveAndFlush(order);
    refunds.revoke(order.getId());
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(e.getExpiresAt()).isEqualTo(result.accessExpiresAt());
  }

  @Test
  void manualIndependentAccessSurvivesProgramWithdrawal() {
    final var admin = user("Admin", "manual-shared-admin@example.com");
    final var student = user("Learner", "manual-shared-student@example.com");
    final var c = course("Course", "manual-shared", admin, PublishStatus.PUBLISHED);
    final var p = program(admin, "manual-shared-program", c);
    final var courseGrant =
        manual.enroll(
            student.getId(), admin.getId(), request(c.getId(), null, false, false, false, false));
    manual.enroll(
        student.getId(), admin.getId(), request(null, p.getId(), false, false, false, false));
    final var selection =
        manual.enroll(
            student.getId(),
            admin.getId(),
            request(c.getId(), p.getId(), true, false, false, false));
    registration.withdraw(
        student.getId(), selection.registrationId(), admin.getId(), "Wrong choice");
    final var e = enrollmentRepository.findById(courseGrant.enrollmentId()).orElseThrow();
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(e.getExpiresAt()).isNull();
  }

  @Test
  void endpointsRequireAdminAndValidateTargetsAndReasons() throws Exception {
    final var admin = user("Admin", "manual-auth-admin@example.com");
    final var student = user("Learner", "manual-auth-student@example.com");
    final var c = course("Course", "manual-auth-course", admin, PublishStatus.PUBLISHED);
    final var studentAuth =
        new UsernamePasswordAuthenticationToken(
            student.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    mvc.perform(get(route(student)).with(authentication(studentAuth)))
        .andExpect(status().isForbidden());
    mvc.perform(
            post(route(student))
                .with(authentication(studentAuth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(request(c.getId(), null, false, true, true, true))))
        .andExpect(status().isForbidden());
    enroll(admin, student, request(null, null, false, false, false, false))
        .andExpect(status().isBadRequest());
    enroll(admin, student, request(c.getId(), UUID.randomUUID(), false, false, false, false))
        .andExpect(status().isBadRequest());
    enroll(admin, student, request(UUID.randomUUID(), null, false, false, false, false))
        .andExpect(status().isNotFound());
    enroll(
            admin,
            student,
            new ManualEnrollmentRequest(
                UUID.randomUUID(), c.getId(), null, false, false, false, false, "   "))
        .andExpect(status().isBadRequest());
    mvc.perform(get(route(student) + "?page=-1").with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isBadRequest());
    assertThat(audits.findAll()).isEmpty();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void simultaneousManualAdmissionsCannotTakeTheSameLastSeat() throws Exception {
    final var tx = new TransactionTemplate(transactions);
    final var ids =
        tx.execute(status -> {
          final var admin = user("Admin", "manual-race-admin@example.com");
          final var a = user("Learner A", "manual-race-a@example.com");
          final var b = user("Learner B", "manual-race-b@example.com");
          final var c = course("One seat", "manual-race", admin, PublishStatus.PUBLISHED);
          c.setCapacity(1);
          courseRepository.saveAndFlush(c);
          return List.of(admin.getId(), a.getId(), b.getId(), c.getId(), c.getTemplate().getId());
        });
    final var executor = Executors.newFixedThreadPool(2);
    final var start = new CountDownLatch(1);
    try {
      final var futures =
          List.of(ids.get(1), ids.get(2)).stream()
              .map(userId ->
                  executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    try {
                      return manual
                              .enroll(
                                  userId,
                                  ids.get(0),
                                  request(ids.get(3), null, false, false, false, false))
                              .changed()
                          ? 200
                          : 0;
                    } catch (ResponseStatusException exception) {
                      return exception.getStatusCode().value();
                    }
                  }))
              .toList();
      start.countDown();
      assertThat(
              List.of(
                  futures.get(0).get(20, TimeUnit.SECONDS),
                  futures.get(1).get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
      tx.executeWithoutResult(status -> {
        assertThat(
                enrollmentRepository.countAvailableSeatsInUse(
                    ids.get(3), EnrollmentStatus.ACTIVE, Instant.now()))
            .isEqualTo(1);
        assertThat(audits.findAll()).hasSize(1);
      });
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(20, TimeUnit.SECONDS);
      tx.executeWithoutResult(status -> {
        audits.deleteAll();
        enrollmentRepository.deleteAll(
            enrollmentRepository.findByCourseIdAndStatus(ids.get(3), EnrollmentStatus.ACTIVE));
        enrollmentRepository.flush();
        courseRepository.deleteById(ids.get(3));
        courseRepository.flush();
        courseTemplateRepository.deleteById(ids.get(4));
        userRepository.deleteAllById(ids.subList(0, 3));
      });
    }
  }

  @Test
  void optionalManualExpiryIsAuditedAndRetrySafeWithoutShorteningExistingAccess() throws Exception {
    final var admin = user("Admin", "manual-expiry-admin@example.com");
    final var learner = user("Learner", "manual-expiry-student@example.com");
    final var c = course("Course", "manual-expiry-course", admin, PublishStatus.PUBLISHED);
    c.setAccessDurationDays(30);
    courseRepository.saveAndFlush(c);
    final var expiry =
        Instant.now().plusSeconds(86400).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    final var body =
        new ManualEnrollmentRequest(
            UUID.randomUUID(),
            c.getId(),
            null,
            false,
            false,
            false,
            false,
            "One-day access approved",
            expiry);
    enroll(admin, learner, body).andExpect(status().isOk());
    final var e =
        enrollmentRepository.findByUserIdAndCourseId(learner.getId(), c.getId()).orElseThrow();
    assertThat(e.getExpiresAt()).isEqualTo(expiry);
    final var originalAudit = audits.findByRequestId(body.requestId()).orElseThrow();
    assertThat(originalAudit.getRequestedExpiresAt()).isEqualTo(expiry);
    enroll(admin, learner, body).andExpect(status().isOk());
    assertThat(audits.findAll()).hasSize(1);
    enroll(
            admin,
            learner,
            new ManualEnrollmentRequest(
                UUID.randomUUID(),
                c.getId(),
                null,
                false,
                false,
                false,
                false,
                "Shorter requested period",
                expiry.minusSeconds(3600)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changed").value(false));
    assertThat(e.getExpiresAt()).isEqualTo(expiry);
    enroll(
            admin,
            learner,
            new ManualEnrollmentRequest(
                UUID.randomUUID(),
                c.getId(),
                null,
                false,
                false,
                false,
                false,
                "Invalid date",
                Instant.now().minusSeconds(10)))
        .andExpect(status().isBadRequest());
    enroll(
            admin,
            learner,
            new ManualEnrollmentRequest(
                body.requestId(),
                c.getId(),
                null,
                false,
                false,
                false,
                false,
                body.reason(),
                expiry.plusSeconds(3600)))
        .andExpect(status().isConflict());
  }
}
