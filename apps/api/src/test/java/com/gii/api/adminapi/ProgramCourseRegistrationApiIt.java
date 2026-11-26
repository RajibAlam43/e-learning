package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gii.api.model.request.admin.SaveProgramCurriculumRequest;
import com.gii.api.model.request.admin.SetCollectionCoursesRequest;
import com.gii.api.model.request.payment.CreateCheckoutOrderItemRequest;
import com.gii.api.model.request.payment.CreateCheckoutOrderRequest;
import com.gii.api.service.academic.CoursePrerequisiteManagementService;
import com.gii.api.service.academic.CourseWaiverService;
import com.gii.api.service.academic.ProgramCourseRegistrationService;
import com.gii.api.service.academic.ProgramCurriculumService;
import com.gii.api.service.academic.StudentProgramCurriculumService;
import com.gii.api.service.admin.AdminCollectionManagementService;
import com.gii.api.service.payment.PendingCartOrderService;
import com.gii.common.entity.academic.Semester;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.ProgramRegistrationStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.academic.ProgramCourseRegistrationRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.service.payment.PaidOrderEnrollmentService;
import com.gii.common.service.payment.RefundedOrderEnrollmentService;
import java.math.BigDecimal;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Transactional
class ProgramCourseRegistrationApiIt extends AbstractAdminApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ProgramCourseRegistrationService registration;
  @Autowired ProgramCurriculumService curriculum;
  @Autowired AdminCollectionManagementService management;
  @Autowired StudentProgramCurriculumService view;
  @Autowired PendingCartOrderService checkout;
  @Autowired PaidOrderEnrollmentService paid;
  @Autowired RefundedOrderEnrollmentService refunds;
  @Autowired CollectionEnrollmentRepository programs;
  @Autowired ProgramCourseRegistrationRepository registrations;
  @Autowired AcademicAchievementRepository achievements;
  @Autowired CoursePrerequisiteManagementService prerequisites;
  @Autowired CourseWaiverService waivers;
  @Autowired PlatformTransactionManager transactions;

  Authentication student(User u) {
    return student(u.getId());
  }

  Authentication student(UUID id) {
    return new UsernamePasswordAuthenticationToken(
        id, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }

  Course cohort(User admin, String slug) {
    final var c = course(slug, slug, admin, PublishStatus.PUBLISHED);
    Instant now = Instant.now();
    final var semester =
        semesterRepository.saveAndFlush(
            Semester.builder()
                .code("S-" + UUID.randomUUID())
                .name("Open semester")
                .startsAt(now.plusSeconds(86400))
                .endsAt(now.plusSeconds(8640000))
                .registrationStartsAt(now.minusSeconds(86400))
                .registrationEndsAt(now.plusSeconds(864000))
                .build());
    c.setStudyMode(StudyMode.COHORT_BASED);
    c.setSemester(semester);
    c.setName(slug);
    c.setStartsAt(semester.getStartsAt());
    c.setEndsAt(semester.getEndsAt());
    return courseRepository.saveAndFlush(c);
  }

  Collection program(User admin, String slug, Course core, Course... electives) {
    final var p = collection(slug, slug, admin, PublishStatus.DRAFT);
    p.setType(CollectionType.DIPLOMA);
    p.setPriceBdt(BigDecimal.valueOf(1000));
    collectionRepository.saveAndFlush(p);
    final var rows = new java.util.ArrayList<SetCollectionCoursesRequest.Item>();
    rows.add(new SetCollectionCoursesRequest.Item(core.getId(), 1, true));
    for (var c : electives) {
      rows.add(new SetCollectionCoursesRequest.Item(c.getId(), rows.size() + 1, false));
    }
    curriculum.save(
        p.getId(),
        new SaveProgramCurriculumRequest(
            electives.length == 0 ? 3 : 6, electives.length == 0 ? 0 : 3, 12, 48, rows));
    management.publish(p.getId());
    return p;
  }

  CollectionEnrollment purchase(User user, Collection program) {
    final var response =
        checkout.execute(
            new CreateCheckoutOrderRequest(
                List.of(
                    new CreateCheckoutOrderItemRequest(
                        OrderItemType.COLLECTION, null, program.getId()))),
            student(user));
    final var order = orderRepository.findById(response.orderId()).orElseThrow();
    order.setStatus(OrderStatus.PAID);
    order.setPaidAt(Instant.now());
    orderRepository.saveAndFlush(order);
    paid.grant(order.getId());
    return programs.findByUserIdAndCollectionId(user.getId(), program.getId()).orElseThrow();
  }

  @Test
  void studentCanChooseAnotherPublishedCohortAndRetryWithoutRenewingOrDuplicating()
      throws Exception {
    final var admin = user("Admin", "registration-admin@example.com");
    final var learner = user("Learner", "registration-student@example.com");
    final var core = cohort(admin, "registration-core");
    final var p = program(admin, "registration-program", core);
    final var purchase = purchase(learner, p);
    final var second =
        courseRepository.saveAndFlush(
            Course.builder()
                .template(core.getTemplate())
                .slug("registration-cohort-two")
                .name("Evening cohort")
                .status(PublishStatus.PUBLISHED)
                .studyMode(StudyMode.COHORT_BASED)
                .semester(core.getSemester())
                .createdBy(admin)
                .build());
    assertThatThrownBy(() ->
            checkout.execute(
                new CreateCheckoutOrderRequest(
                    List.of(
                        new CreateCheckoutOrderItemRequest(
                            OrderItemType.COURSE, second.getId(), null))),
                student(learner)))
        .hasMessageContaining("Open My Programs");
    core.setStatus(PublishStatus.DRAFT);
    courseRepository.saveAndFlush(core);
    mvc.perform(
            post("/student/collections/" + p.getId() + "/registrations")
                .with(authentication(student(learner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseId\":\"" + second.getId() + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cohortName").value("Evening cohort"));
    final var e =
        enrollmentRepository.findByUserIdAndCourseId(learner.getId(), second.getId()).orElseThrow();
    assertThat(e.getExpiresAt()).isEqualTo(purchase.getExpiresAt());
    final var first = registration.register(learner.getId(), p.getId(), second.getId());
    final var again = registration.register(learner.getId(), p.getId(), second.getId());
    assertThat(again.id()).isEqualTo(first.id());
    assertThat(again.accessExpiresAt()).isEqualTo(first.accessExpiresAt());
    assertThat(
            registrations.findByProgramEnrollmentIdAndStatus(
                purchase.getId(), ProgramRegistrationStatus.ACTIVE))
        .hasSize(1);
    mvc.perform(
            get("/student/collections/" + p.getId() + "/curriculum")
                .with(authentication(student(learner))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availableCourses[0].enrolled").value(true))
        .andExpect(jsonPath("$.availableCourses[0].courseId").value(second.getId().toString()));
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), core.getId()))
        .hasMessageContaining("already selected");
  }

  @Test
  void unrelatedDraftAndInactiveProgramCannotBeUsedForRegistration() throws Exception {
    final var admin = user("Admin", "scope-admin@example.com");
    final var learner = user("Learner", "scope-student@example.com");
    final var other = user("Other", "scope-other@example.com");
    final var core = cohort(admin, "scope-core");
    final var unrelated = cohort(admin, "scope-unrelated");
    final var p = program(admin, "scope-program", core);
    final var e = purchase(learner, p);
    assertThatThrownBy(() -> registration.register(other.getId(), p.getId(), core.getId()))
        .hasMessageContaining("Program enrollment not found");
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), unrelated.getId()))
        .hasMessageContaining("not included");
    core.setStatus(PublishStatus.DRAFT);
    courseRepository.saveAndFlush(core);
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), core.getId()))
        .hasMessageContaining("not published");
    e.setExpiresAt(Instant.now().minusSeconds(1));
    programs.saveAndFlush(e);
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), core.getId()))
        .hasMessageContaining("expired");
    mvc.perform(
            post("/student/collections/" + p.getId() + "/registrations")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseId\":\"" + core.getId() + "\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void prerequisitesAreEnforcedAndApprovedWaiverAllowsRegistration() {
    final var admin = user("Admin", "prereq-reg-admin@example.com");
    final var learner = user("Learner", "prereq-reg-student@example.com");
    final var core = cohort(admin, "prereq-reg-core");
    final var required = cohort(admin, "prereq-reg-required");
    final var p = program(admin, "prereq-reg-program", core);
    purchase(learner, p);
    prerequisites.save(core.getId(), List.of(required.getTemplate().getId()));
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), core.getId()))
        .hasMessageContaining("credit waiver");
    final var w =
        waivers.create(
            learner.getId(),
            new com.gii.api.model.request.admin.CreateWaiverRequest(
                required.getTemplate().getId(), "Equivalent study", null));
    waivers.review(
        w.id(),
        admin.getId(),
        new com.gii.api.model.request.admin.ReviewWaiverRequest(
            com.gii.common.enums.WaiverStatus.APPROVED, "Verified", 3, null));
    assertThat(registration.register(learner.getId(), p.getId(), core.getId()).courseId())
        .isEqualTo(core.getId());
  }

  @Test
  void semesterOfferingDatesAndCapacityAreEnforcedAtRegistration() {
    final var admin = user("Admin", "window-reg-admin@example.com");
    final var learner = user("Learner", "window-reg-student@example.com");
    final var core = cohort(admin, "window-reg-core");
    final var p = program(admin, "window-reg-program", core);
    purchase(learner, p);
    core.getSemester().setRegistrationStartsAt(Instant.now().plusSeconds(3600));
    semesterRepository.saveAndFlush(core.getSemester());
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), core.getId()))
        .hasMessageContaining("not opened");
    core.getSemester().setRegistrationStartsAt(Instant.now().minusSeconds(86400));
    semesterRepository.saveAndFlush(core.getSemester());
    core.setEnrollmentEndsAt(Instant.now().minusSeconds(60));
    courseRepository.saveAndFlush(core);
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), core.getId()))
        .hasMessageContaining("closed");
    core.setEnrollmentEndsAt(null);
    core.setCapacity(1);
    courseRepository.saveAndFlush(core);
    final var occupant = user("Occupied", "window-reg-occupied@example.com");
    enrollment(occupant, core, EnrollmentStatus.ACTIVE);
    enrollmentRepository.flush();
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), core.getId()))
        .hasMessageContaining("capacity");
  }

  @Test
  void electiveReservationsAreBoundedAndAdminWithdrawalReleasesThemWithAudit() throws Exception {
    final var admin = user("Admin", "budget-reg-admin@example.com");
    final var learner = user("Learner", "budget-reg-student@example.com");
    final var core = cohort(admin, "budget-reg-core");
    final var first = cohort(admin, "budget-reg-first");
    final var next = cohort(admin, "budget-reg-next");
    final var p = program(admin, "budget-reg-program", core, first, next);
    purchase(learner, p);
    final var choice = registration.register(learner.getId(), p.getId(), first.getId());
    assertThat(view.get(learner.getId(), p.getId()).electiveAllowance().remaining()).isZero();
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), next.getId()))
        .hasMessageContaining("elective credit allowance");
    final var separate =
        checkout.execute(
            new CreateCheckoutOrderRequest(
                List.of(
                    new CreateCheckoutOrderItemRequest(OrderItemType.COURSE, next.getId(), null))),
            student(learner));
    assertThat(separate.orderId()).isNotNull();
    mvc.perform(
            post("/admin/students/"
                    + learner.getId()
                    + "/program-registrations/"
                    + choice.id()
                    + "/withdraw")
                .with(authentication(student(learner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Change cohort\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/admin/students/"
                    + learner.getId()
                    + "/program-registrations/"
                    + choice.id()
                    + "/withdraw")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Approved elective change\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.withdrawnByName").value("Admin"));
    assertThat(view.get(learner.getId(), p.getId()).electiveAllowance().remaining()).isEqualTo(3);
    assertThat(
            enrollmentRepository
                .findByUserIdAndCourseId(learner.getId(), first.getId())
                .orElseThrow()
                .getStatus())
        .isEqualTo(EnrollmentStatus.REVOKED);
    registration.register(learner.getId(), p.getId(), next.getId());
    assertThat(registration.history(learner.getId())).hasSize(2);
  }

  @Test
  void completedOrCreditedSelectionsCannotBeWithdrawnOrChargedTwice() {
    final var admin = user("Admin", "completed-reg-admin@example.com");
    final var learner = user("Learner", "completed-reg-student@example.com");
    final var core = cohort(admin, "completed-reg-core");
    final var elective = cohort(admin, "completed-reg-elective");
    final var p = program(admin, "completed-reg-program", core, elective);
    purchase(learner, p);
    final var choice = registration.register(learner.getId(), p.getId(), elective.getId());
    final var e =
        enrollmentRepository
            .findByUserIdAndCourseId(learner.getId(), elective.getId())
            .orElseThrow();
    e.setCompletedAt(Instant.now());
    enrollmentRepository.saveAndFlush(e);
    achievements.awardCompletion(e.getId());
    assertThat(view.get(learner.getId(), p.getId()).electiveAllowance().used()).isEqualTo(3);
    assertThatThrownBy(() ->
            registration.withdraw(learner.getId(), choice.id(), admin.getId(), "Release credits"))
        .hasMessageContaining("cannot be withdrawn");
  }

  @Test
  void reusedElectiveCreditsConsumeAllowanceAndRegistrationCannotAddMoreChoices() {
    final var admin = user("Admin", "reuse-reg-admin@example.com");
    final var learner = user("Learner", "reuse-reg-student@example.com");
    final var core = cohort(admin, "reuse-reg-core");
    final var earned = cohort(admin, "reuse-reg-earned");
    final var next = cohort(admin, "reuse-reg-next");
    final var p = program(admin, "reuse-reg-program", core, earned, next);
    purchase(learner, p);
    final var e = enrollment(learner, earned, EnrollmentStatus.ACTIVE);
    e.setCompletedAt(Instant.now());
    enrollmentRepository.saveAndFlush(e);
    achievements.awardCompletion(e.getId());
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), next.getId()))
        .hasMessageContaining("elective credit allowance");
    assertThatThrownBy(() -> registration.register(learner.getId(), p.getId(), earned.getId()))
        .hasMessageContaining("already have academic credit");
  }

  @Test
  void courseAccessIsCappedAndExpiredProgramRegistrationCannotReadProtectedLessons()
      throws Exception {
    final var admin = user("Admin", "expiry-reg-admin@example.com");
    final var learner = user("Learner", "expiry-reg-student@example.com");
    final var core = cohort(admin, "expiry-reg-core");
    core.setAccessDurationDays(7);
    courseRepository.saveAndFlush(core);
    final var p = program(admin, "expiry-reg-program", core);
    final var bought = purchase(learner, p);
    Instant expiry = Instant.now().plusSeconds(3600);
    bought.setExpiresAt(expiry);
    programs.saveAndFlush(bought);
    registration.register(learner.getId(), p.getId(), core.getId());
    final var e =
        enrollmentRepository.findByUserIdAndCourseId(learner.getId(), core.getId()).orElseThrow();
    assertThat(e.getExpiresAt()).isEqualTo(expiry);
    final var section = section(core, 1);
    section.setStatus(PublishStatus.PUBLISHED);
    courseSectionRepository.saveAndFlush(section);
    final var lesson = lesson(core, section, 1);
    lesson.setStatus(PublishStatus.PUBLISHED);
    lessonRepository.saveAndFlush(lesson);
    e.setExpiresAt(Instant.now().minusSeconds(1));
    enrollmentRepository.saveAndFlush(e);
    mvc.perform(
            get("/learn/courses/" + core.getId() + "/lessons/" + lesson.getId())
                .with(authentication(student(learner))))
        .andExpect(status().isForbidden());
  }

  @Test
  void independentCourseAccessSurvivesWithdrawalOfProgramChoice() {
    final var admin = user("Admin", "independent-reg-admin@example.com");
    final var learner = user("Learner", "independent-reg-student@example.com");
    final var core = cohort(admin, "independent-reg-core");
    final var elective = cohort(admin, "independent-reg-elective");
    final var p = program(admin, "independent-reg-program", core, elective);
    purchase(learner, p);
    final var e = enrollment(learner, elective, EnrollmentStatus.ACTIVE);
    Instant expiry = Instant.now().plusSeconds(864000000);
    e.setExpiresAt(expiry);
    enrollmentRepository.saveAndFlush(e);
    final var choice = registration.register(learner.getId(), p.getId(), elective.getId());
    assertThat(e.getExpiresAt()).isEqualTo(expiry);
    registration.withdraw(
        learner.getId(), choice.id(), admin.getId(), "Different elective requested");
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(e.getExpiresAt()).isEqualTo(expiry);
  }

  @Test
  void refundPreservesAnotherProgramGrantThenRemovesAccessWhenAllGrantsEnd() {
    final var admin = user("Admin", "shared-reg-admin@example.com");
    final var learner = user("Learner", "shared-reg-student@example.com");
    final var core = cohort(admin, "shared-reg-core");
    core.setCapacity(1);
    courseRepository.saveAndFlush(core);
    final var p1 = program(admin, "shared-reg-one", core);
    final var p2 = program(admin, "shared-reg-two", core);
    final var a = purchase(learner, p1);
    final var b = purchase(learner, p2);
    a.setExpiresAt(Instant.now().plusSeconds(864000));
    b.setExpiresAt(Instant.now().plusSeconds(1728000));
    programs.saveAndFlush(a);
    programs.saveAndFlush(b);
    registration.register(learner.getId(), p1.getId(), core.getId());
    assertThat(
            view.get(learner.getId(), p2.getId())
                .availableCourses()
                .getFirst()
                .offerings()
                .getFirst()
                .canRegister())
        .isTrue();
    registration.register(learner.getId(), p2.getId(), core.getId());
    final var e =
        enrollmentRepository.findByUserIdAndCourseId(learner.getId(), core.getId()).orElseThrow();
    assertThat(e.getExpiresAt()).isEqualTo(b.getExpiresAt());
    final var firstOrder = a.getSourceOrderItem().getOrder();
    firstOrder.setStatus(OrderStatus.REFUNDED);
    orderRepository.saveAndFlush(firstOrder);
    refunds.revoke(firstOrder.getId());
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(e.getExpiresAt()).isEqualTo(b.getExpiresAt());
    final var secondOrder = b.getSourceOrderItem().getOrder();
    secondOrder.setStatus(OrderStatus.REFUNDED);
    orderRepository.saveAndFlush(secondOrder);
    refunds.revoke(secondOrder.getId());
    assertThat(e.getStatus()).isNotEqualTo(EnrollmentStatus.ACTIVE);
  }

  @Test
  void selectedLaterAdditionStaysAvailableWhenRemovedFromCurrentCatalogue() {
    final var admin = user("Admin", "removed-reg-admin@example.com");
    final var learner = user("Learner", "removed-reg-student@example.com");
    final var core = cohort(admin, "removed-reg-core");
    final var elective = cohort(admin, "removed-reg-elective");
    final var p = program(admin, "removed-reg-program", core);
    curriculum.save(
        p.getId(),
        new SaveProgramCurriculumRequest(
            3, 3, 12, 48, List.of(new SetCollectionCoursesRequest.Item(core.getId(), 1, true))));
    purchase(learner, p);
    curriculum.save(
        p.getId(),
        new SaveProgramCurriculumRequest(
            6,
            3,
            12,
            48,
            List.of(
                new SetCollectionCoursesRequest.Item(core.getId(), 1, true),
                new SetCollectionCoursesRequest.Item(elective.getId(), 2, false))));
    registration.register(learner.getId(), p.getId(), elective.getId());
    curriculum.save(
        p.getId(),
        new SaveProgramCurriculumRequest(
            3, 3, 12, 48, List.of(new SetCollectionCoursesRequest.Item(core.getId(), 1, true))));
    assertThat(view.get(learner.getId(), p.getId()).availableCourses())
        .anySatisfy(c -> {
          assertThat(c.courseTemplateId()).isEqualTo(elective.getTemplate().getId());
          assertThat(c.enrolled()).isTrue();
        });
  }

  @Test
  void legacyPurchaserRetainsFundingWhenProgramIsConvertedAndCoursesAreAdded() {
    final var admin = user("Admin", "legacy-funding-admin@example.com");
    final var learner = user("Learner", "legacy-funding-student@example.com");
    final var core = cohort(admin, "legacy-funding-core");
    final var added = cohort(admin, "legacy-funding-added");
    final var p = collection("Legacy", "legacy-funding-program", admin, PublishStatus.DRAFT);
    collectionCourse(p, core, 1, true);
    management.publish(p.getId());
    purchase(learner, p);
    curriculum.save(
        p.getId(),
        new SaveProgramCurriculumRequest(
            6,
            3,
            12,
            48,
            List.of(
                new SetCollectionCoursesRequest.Item(core.getId(), 1, true),
                new SetCollectionCoursesRequest.Item(added.getId(), 2, false))));
    assertThatThrownBy(() ->
            checkout.execute(
                new CreateCheckoutOrderRequest(
                    List.of(
                        new CreateCheckoutOrderItemRequest(
                            OrderItemType.COURSE, added.getId(), null))),
                student(learner)))
        .hasMessageContaining("Open My Programs");
    assertThat(registration.register(learner.getId(), p.getId(), added.getId()).courseId())
        .isEqualTo(added.getId());
  }

  @Test
  void cartCannotChargeForBothProgramAndItsFundableCourse() {
    final var admin = user("Admin", "cart-funding-admin@example.com");
    final var learner = user("Learner", "cart-funding-student@example.com");
    final var core = cohort(admin, "cart-funding-core");
    final var p = program(admin, "cart-funding-program", core);
    assertThatThrownBy(() ->
            checkout.execute(
                new CreateCheckoutOrderRequest(
                    List.of(
                        new CreateCheckoutOrderItemRequest(
                            OrderItemType.COLLECTION, null, p.getId()),
                        new CreateCheckoutOrderItemRequest(
                            OrderItemType.COURSE, core.getId(), null))),
                student(learner)))
        .hasMessageContaining("remove its separate purchase");
  }

  record Race(UUID programId, UUID firstCourse, UUID nextCourse, UUID firstUser, UUID nextUser) {}

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentChoicesCannotOverspendElectiveAllowance() throws Exception {
    race(false);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentStudentsCannotTakeTheLastSeatTwice() throws Exception {
    race(true);
  }

  void race(boolean capacity) throws Exception {
    try {
      Race fixture =
          new TransactionTemplate(transactions)
              .execute(tx -> {
                final var admin = user("Race admin", "reg-race-admin@example.com");
                final var one = user("One", "reg-race-one@example.com");
                final var two = capacity ? user("Two", "reg-race-two@example.com") : one;
                final var core = cohort(admin, "reg-race-core");
                final var first = capacity ? core : cohort(admin, "reg-race-first");
                final var next = capacity ? core : cohort(admin, "reg-race-next");
                if (capacity) {
                  core.setCapacity(1);
                  courseRepository.saveAndFlush(core);
                }
                final var p =
                    capacity
                        ? program(admin, "reg-race-program", core)
                        : program(admin, "reg-race-program", core, first, next);
                purchase(one, p);
                if (capacity) {
                  purchase(two, p);
                }
                return new Race(p.getId(), first.getId(), next.getId(), one.getId(), two.getId());
              });
      final var start = new CountDownLatch(1);
      try (var executor = Executors.newFixedThreadPool(2)) {
        final var first =
            executor.submit(() -> {
              start.await();
              return tryRegistration(
                  fixture.firstUser(), fixture.programId(), fixture.firstCourse());
            });
        final var next =
            executor.submit(() -> {
              start.await();
              return tryRegistration(fixture.nextUser(), fixture.programId(), fixture.nextCourse());
            });
        start.countDown();
        assertThat(first.get(15, TimeUnit.SECONDS) ^ next.get(15, TimeUnit.SECONDS)).isTrue();
      }
    } finally {
      registrations.deleteAll();
      programs.deleteAll();
      enrollmentRepository.deleteAll();
      orderItemCourseRepository.deleteAll();
      orderItemRepository.deleteAll();
      orderRepository.deleteAll();
      cleanupAdminData();
    }
  }

  boolean tryRegistration(UUID user, UUID program, UUID course) {
    try {
      registration.register(user, program, course);
      return true;
    } catch (ResponseStatusException exception) {
      assertThat(exception.getStatusCode().value()).isEqualTo(409);
      return false;
    }
  }
}
