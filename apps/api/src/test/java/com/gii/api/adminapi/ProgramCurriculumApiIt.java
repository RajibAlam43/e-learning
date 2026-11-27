package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.model.request.admin.SaveProgramCurriculumRequest;
import com.gii.api.model.request.admin.SetCollectionCoursesRequest;
import com.gii.api.model.request.payment.CreateCheckoutOrderItemRequest;
import com.gii.api.model.request.payment.CreateCheckoutOrderRequest;
import com.gii.api.service.academic.ProgramCurriculumService;
import com.gii.api.service.admin.AdminCollectionManagementService;
import com.gii.api.service.collection.PurchasedCollectionCoursesService;
import com.gii.api.service.payment.PendingCartOrderService;
import com.gii.api.service.payment.PendingOrderEligibilityService;
import com.gii.api.service.progress.ProgramCreditProgressService;
import com.gii.common.entity.collection.Collection;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CollectionType;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.service.payment.PaidOrderEnrollmentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProgramCurriculumApiIt extends AbstractAdminApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ProgramCurriculumService curriculum;
  @Autowired AdminCollectionManagementService management;
  @Autowired PendingCartOrderService checkout;
  @Autowired PendingOrderEligibilityService pending;
  @Autowired PaidOrderEnrollmentService paid;
  @Autowired CollectionEnrollmentRepository enrollments;
  @Autowired PurchasedCollectionCoursesService purchased;
  @Autowired AcademicAchievementRepository achievements;
  @Autowired ProgramCreditProgressService progress;
  @Autowired JdbcTemplate jdbc;
  @Autowired com.gii.api.service.academic.CourseWaiverService waivers;
  @Autowired com.gii.common.service.payment.RefundedOrderEnrollmentService refunds;
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void unreachableWholeCourseElectiveCombinationsAreRejected() {
    final var admin = user("Admin", "program-combination-admin@example.com");
    final var p = program(admin, "program-combination");
    final var core = course("Core", "program-combination-core", admin, PublishStatus.DRAFT);
    final var a = course("A", "program-combination-a", admin, PublishStatus.DRAFT);
    final var b = course("B", "program-combination-b", admin, PublishStatus.DRAFT);
    // Core 3 + a cap of 4 cannot fund both 3-credit electives to reach 7.
    assertThatThrownBy(() -> curriculum.save(p.getId(), request(7, 4, core, a, b)))
        .hasMessageContaining("cannot be reached");
  }

  Authentication student(User u) {
    return new UsernamePasswordAuthenticationToken(
        u.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }

  Collection program(User u, String slug) {
    var p = collection("Diploma", slug, u, PublishStatus.DRAFT);
    p.setType(CollectionType.DIPLOMA);
    p.setPriceBdt(BigDecimal.valueOf(1000));
    return collectionRepository.saveAndFlush(p);
  }

  SaveProgramCurriculumRequest request(int required, int cap, Course... courses) {
    var items = new java.util.ArrayList<SetCollectionCoursesRequest.Item>();
    for (int i = 0; i < courses.length; i++) {
      items.add(new SetCollectionCoursesRequest.Item(courses[i].getId(), i + 1, i == 0));
    }
    return new SaveProgramCurriculumRequest(required, cap, 12, null, items);
  }

  CollectionEnrollment purchase(Collection p, User learner) {
    var result =
        checkout.execute(
            new CreateCheckoutOrderRequest(
                List.of(
                    new CreateCheckoutOrderItemRequest(OrderItemType.COLLECTION, null, p.getId()))),
            student(learner));
    var order = orderRepository.findById(result.orderId()).orElseThrow();
    order.setStatus(OrderStatus.PAID);
    order.setPaidAt(Instant.parse("2026-01-31T12:00:00Z"));
    orderRepository.saveAndFlush(order);
    paid.grant(order.getId());
    return enrollments.findByUserIdAndCollectionId(learner.getId(), p.getId()).orElseThrow();
  }

  void award(User learner, Course c) {
    var e = enrollment(learner, c, EnrollmentStatus.ACTIVE);
    e.setCompletedAt(Instant.now());
    enrollmentRepository.saveAndFlush(e);
    achievements.awardCompletion(e.getId());
  }

  @Test
  void adminCanConfigurePublishAndEditProgramsWithPlannedCourses() throws Exception {
    var admin = user("Program Admin", "program-admin@example.com");
    var p = program(admin, "planned-diploma");
    var a = course("Foundation", "program-foundation", admin, PublishStatus.DRAFT);
    var b = course("Elective", "program-elective", admin, PublishStatus.DRAFT);
    mvc.perform(
            put("/admin/collections/" + p.getId() + "/curriculum")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request(6, 3, a, b))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.programRequirements.accessDurationMonths").value(36))
        .andExpect(jsonPath("$.courses[0].status").value("DRAFT"));
    mvc.perform(
            post("/admin/collections/" + p.getId() + "/publish")
                .with(authentication(adminAuth(admin.getId()))))
        .andExpect(status().isOk());
    var c = course("Future elective", "future-elective", admin, PublishStatus.DRAFT);
    curriculum.save(p.getId(), request(6, 3, a, b, c));
    mvc.perform(get("/public/collections/planned-diploma"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courses.length()").value(3))
        .andExpect(jsonPath("$.courses[0].available").value(false))
        .andExpect(jsonPath("$.courses[0].slug").doesNotExist());
    mvc.perform(get("/public/courses/program-foundation")).andExpect(status().isNotFound());
    mvc.perform(
            put("/admin/collections/" + p.getId() + "/curriculum")
                .with(authentication(student(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request(6, 3, a, b))))
        .andExpect(status().isForbidden());
  }

  @Test
  void validationRejectsUnreachableCreditsDuplicateCurriculaAndInvalidDurations() throws Exception {
    var admin = user("Rules Admin", "program-rules@example.com");
    var p = program(admin, "program-rules");
    var a = course("A", "rules-a", admin);
    var b = course("B", "rules-b", admin);
    assertThatThrownBy(() -> curriculum.save(p.getId(), request(7, 3, a, b)))
        .hasMessageContaining("cannot be reached");
    mvc.perform(
            put("/admin/collections/" + p.getId() + "/curriculum")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"requiredCredits\":3,\"electiveCreditLimit\":0,"
                        + "\"expectedDurationMonths\":0,\"items\":[{\"courseId\":\""
                        + a.getId()
                        + "\",\"position\":1}]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void duplicateCohortsDoNotCountAsTwoCourses() {
    var admin = user("Duplicate Admin", "program-duplicate@example.com");
    var p = program(admin, "program-duplicate");
    var a = course("A", "duplicate-a", admin);
    var b =
        courseRepository.saveAndFlush(
            Course.builder()
                .template(a.getTemplate())
                .slug("duplicate-b")
                .createdBy(admin)
                .build());
    assertThatThrownBy(() -> curriculum.save(p.getId(), request(6, 3, a, b)))
        .hasMessageContaining("only once");
  }

  @Test
  void purchaseDefersIndividualEnrollmentAndUsesConfirmedPaymentForFixedExpiry() {
    var admin = user("Payment Admin", "program-payment-admin@example.com");
    final var learner = user("Learner", "program-payment-student@example.com");
    var p = program(admin, "program-payment");
    var a = course("Future course", "program-future", admin, PublishStatus.DRAFT);
    a.setEnrollmentEndsAt(Instant.parse("2020-01-01T00:00:00Z"));
    courseRepository.saveAndFlush(a);
    curriculum.save(p.getId(), request(3, 0, a));
    management.publish(p.getId());
    var e = purchase(p, learner);
    assertThat(e.getExpiresAt()).isEqualTo(Instant.parse("2029-01-31T12:00:00Z"));
    assertThat(e.getEnrolledAt()).isEqualTo(Instant.parse("2026-01-31T12:00:00Z"));
    assertThat(enrollmentRepository.findByUserIdAndCourseId(learner.getId(), a.getId())).isEmpty();
    paid.grant(e.getSourceOrderItem().getOrder().getId());
    assertThat(e.getExpiresAt()).isEqualTo(Instant.parse("2029-01-31T12:00:00Z"));
  }

  @Test
  void laterAdditionsAndSettingsDoNotChangePurchasedRequirementsOrPendingContracts()
      throws Exception {
    var admin = user("Snapshot Admin", "program-snapshot-admin@example.com");
    final var learner = user("Learner", "program-snapshot-student@example.com");
    var p = program(admin, "program-snapshot");
    var a = course("Original core", "snapshot-core", admin, PublishStatus.DRAFT);
    var b = course("Original elective", "snapshot-elective", admin, PublishStatus.DRAFT);
    curriculum.save(p.getId(), request(6, 3, a, b));
    management.publish(p.getId());
    var order =
        checkout.execute(
            new CreateCheckoutOrderRequest(
                List.of(
                    new CreateCheckoutOrderItemRequest(OrderItemType.COLLECTION, null, p.getId()))),
            student(learner));
    var c = course("New core", "snapshot-new", admin, PublishStatus.DRAFT);
    curriculum.save(
        p.getId(),
        new SaveProgramCurriculumRequest(
            9,
            3,
            24,
            60,
            List.of(
                new SetCollectionCoursesRequest.Item(a.getId(), 1, true),
                new SetCollectionCoursesRequest.Item(b.getId(), 2, false),
                new SetCollectionCoursesRequest.Item(c.getId(), 3, true))));
    var entity = orderRepository.findById(order.orderId()).orElseThrow();
    pending.validate(entity);
    entity.setStatus(OrderStatus.PAID);
    entity.setPaidAt(Instant.parse("2026-01-31T12:00:00Z"));
    orderRepository.saveAndFlush(entity);
    paid.grant(entity.getId());
    var e = enrollments.findByUserIdAndCollectionId(learner.getId(), p.getId()).orElseThrow();
    assertThat(e.getProgramSnapshot().requiredCredits()).isEqualTo(6);
    assertThat(e.getProgramSnapshot().courses()).hasSize(2);
    assertThat(e.getExpiresAt()).isEqualTo(Instant.parse("2029-01-31T12:00:00Z"));
    assertThat(purchased.resolveItems(e))
        .hasSize(3)
        .anySatisfy(i -> {
          assertThat(i.course().getId()).isEqualTo(c.getId());
          assertThat(i.mandatory()).isFalse();
        });
    mvc.perform(
            get("/student/collections/" + p.getId() + "/curriculum")
                .with(authentication(student(learner))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.graduationRequirements.requiredCredits").value(6))
        .andExpect(jsonPath("$.availableCourses.length()").value(3));
    var other = user("Other", "program-other@example.com");
    mvc.perform(
            get("/student/collections/" + p.getId() + "/curriculum")
                .with(authentication(student(other))))
        .andExpect(status().isNotFound());
  }

  @Test
  void onlyEligibleCreditsCountElectivesAreCappedAndMandatoryCoursesCannotBeSkipped() {
    var admin = user("Credit Admin", "program-credit-admin@example.com");
    final var learner = user("Learner", "program-credit-student@example.com");
    var p = program(admin, "program-credits");
    var core = course("Core", "credit-core", admin, PublishStatus.PUBLISHED);
    var e1 = course("Elective One", "credit-elective-one", admin, PublishStatus.PUBLISHED);
    var e2 = course("Elective Two", "credit-elective-two", admin, PublishStatus.PUBLISHED);
    final var unrelated = course("Unrelated", "credit-unrelated", admin, PublishStatus.PUBLISHED);
    curriculum.save(p.getId(), request(6, 3, core, e1, e2));
    management.publish(p.getId());
    final var enrollment = purchase(p, learner);
    award(learner, e1);
    award(learner, e2);
    award(learner, unrelated);
    var partial = progress.get(enrollment);
    assertThat(partial.earnedCredits()).isEqualTo(3);
    assertThat(partial.electiveCreditsCounted()).isEqualTo(3);
    assertThat(partial.complete()).isFalse();
    award(learner, core);
    assertThat(progress.get(enrollment).complete()).isTrue();
    assertThat(progress.get(enrollment).earnedCredits()).isEqualTo(6);
  }

  @Test
  void explicitAccessDurationAndWaiverCreditsCanBeReusedAcrossPrograms() {
    var admin = user("Reuse Admin", "program-reuse-admin@example.com");
    final var learner = user("Learner", "program-reuse-student@example.com");
    var a = course("Shared core", "program-shared-core", admin, PublishStatus.PUBLISHED);
    var p1 = program(admin, "reuse-one");
    var p2 = program(admin, "reuse-two");
    curriculum.save(
        p1.getId(),
        new SaveProgramCurriculumRequest(
            3, 0, 12, 48, List.of(new SetCollectionCoursesRequest.Item(a.getId(), 1, true))));
    curriculum.save(p2.getId(), request(3, 0, a));
    management.publish(p1.getId());
    management.publish(p2.getId());
    var one = purchase(p1, learner);
    final var two = purchase(p2, learner);
    var waiver =
        waivers.create(
            learner.getId(),
            new com.gii.api.model.request.admin.CreateWaiverRequest(
                a.getTemplate().getId(), "Equivalent prior study", null));
    waivers.review(
        waiver.id(),
        admin.getId(),
        new com.gii.api.model.request.admin.ReviewWaiverRequest(
            com.gii.common.enums.WaiverStatus.APPROVED, "Equivalent curriculum verified", 3, null));
    assertThat(one.getExpiresAt()).isEqualTo(Instant.parse("2030-01-31T12:00:00Z"));
    assertThat(progress.get(one).complete()).isTrue();
    assertThat(progress.get(two).complete()).isTrue();
  }

  @Test
  void programOrdersDoNotReserveSeatsOrActAsIndividualCourseEntitlements() {
    var admin = user("Reservation Admin", "program-reservation-admin@example.com");
    final var learner = user("Learner", "program-reservation-student@example.com");
    var p = program(admin, "program-reservation");
    var a = course("Limited cohort", "program-limited", admin, PublishStatus.PUBLISHED);
    a.setCapacity(1);
    courseRepository.saveAndFlush(a);
    curriculum.save(p.getId(), request(3, 0, a));
    management.publish(p.getId());
    var result =
        checkout.execute(
            new CreateCheckoutOrderRequest(
                List.of(
                    new CreateCheckoutOrderItemRequest(OrderItemType.COLLECTION, null, p.getId()))),
            student(learner));
    assertThat(
            orderItemRepository.countPendingReservations(
                a.getId(), UUID.randomUUID(), Instant.now().minusSeconds(300)))
        .isZero();
    var order = orderRepository.findById(result.orderId()).orElseThrow();
    order.setStatus(OrderStatus.PAID);
    order.setPaidAt(Instant.now());
    orderRepository.saveAndFlush(order);
    assertThat(
            orderItemRepository.findPaidCourseEntitlementsExcludingOrder(
                learner.getId(), a.getId(), UUID.randomUUID()))
        .isEmpty();
  }

  @Test
  void refundOfRenewalRestoresOriginalExpiryWithoutChangingGraduationRequirements() {
    var admin = user("Refund Admin", "program-refund-admin@example.com");
    final var learner = user("Learner", "program-refund-student@example.com");
    var p = program(admin, "program-refund");
    var a = course("Core", "program-refund-core", admin, PublishStatus.PUBLISHED);
    curriculum.save(p.getId(), request(3, 0, a));
    management.publish(p.getId());
    var first = purchase(p, learner);
    var original = first.getSourceOrderItem();
    original.getOrder().setPaidAt(Instant.parse("2000-01-31T12:00:00Z"));
    orderRepository.saveAndFlush(original.getOrder());
    first.setExpiresAt(Instant.parse("2003-01-31T12:00:00Z"));
    enrollments.saveAndFlush(first);
    var renewed = purchase(p, learner);
    var renewalOrder = renewed.getSourceOrderItem().getOrder();
    renewalOrder.setStatus(OrderStatus.REFUNDED);
    orderRepository.saveAndFlush(renewalOrder);
    refunds.revoke(renewalOrder.getId());
    assertThat(renewed.getExpiresAt()).isEqualTo(Instant.parse("2003-01-31T12:00:00Z"));
    assertThat(renewed.getProgramSnapshot().requiredCredits()).isEqualTo(3);
  }

  @Test
  void databaseRejectsChangingAnExistingPurchasedGraduationContract() {
    var admin = user("Immutable Admin", "program-immutable-admin@example.com");
    final var learner = user("Learner", "program-immutable-student@example.com");
    var p = program(admin, "program-immutable");
    var a = course("Core", "program-immutable-core", admin, PublishStatus.DRAFT);
    curriculum.save(p.getId(), request(3, 0, a));
    management.publish(p.getId());
    var e = purchase(p, learner);
    assertThatThrownBy(() ->
            jdbc.update(
                """
                UPDATE collection_enrollments
                SET program_snapshot=jsonb_set(program_snapshot,'{requiredCredits}','999'::jsonb)
                WHERE id=?
                """,
                e.getId()))
        .hasMessageContaining("Purchased program requirements are immutable");
  }

  @Test
  void historicalBackfillPrefersPurchasedCoursesAndSupportsManualEnrollmentFallback()
      throws Exception {
    var admin = user("Migration Admin", "program-migration-admin@example.com");
    final var learner = user("Learner", "program-migration-student@example.com");
    var p = program(admin, "program-migration");
    var original = course("Original", "program-migration-original", admin, PublishStatus.PUBLISHED);
    var added = course("Later addition", "program-migration-added", admin, PublishStatus.PUBLISHED);
    collectionCourse(p, added, 1, true);
    var order =
        orderRepository.saveAndFlush(
            com.gii.common.entity.order.Order.builder()
                .user(learner)
                .amountBdt(BigDecimal.valueOf(1000))
                .provider(com.gii.common.enums.OrderProvider.SSLCOMMERZ)
                .status(OrderStatus.PAID)
                .paidAt(Instant.now())
                .build());
    var item =
        orderItemRepository.saveAndFlush(
            com.gii.common.entity.order.OrderItem.builder()
                .order(order)
                .collection(p)
                .itemType(OrderItemType.COLLECTION)
                .titleSnapshot("Original diploma")
                .priceBdt(BigDecimal.valueOf(1000))
                .build());
    orderItemCourseRepository.saveAndFlush(
        com.gii.common.entity.order.OrderItemCourse.builder()
            .orderItem(item)
            .course(original)
            .position(1)
            .isMandatory(true)
            .build());
    final var e =
        enrollments.saveAndFlush(
            CollectionEnrollment.builder()
                .user(learner)
                .collection(p)
                .sourceOrderItem(item)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .build());
    var manualUser = user("Manual", "program-migration-manual@example.com");
    final var manual =
        enrollments.saveAndFlush(
            CollectionEnrollment.builder()
                .user(manualUser)
                .collection(p)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .build());
    String sql;
    try (var input =
        getClass().getResourceAsStream("/db/migration/V26__program_curriculum_requirements.sql")) {
      sql = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
    sql = sql.substring(sql.indexOf("UPDATE order_items"), sql.indexOf("CREATE FUNCTION"));
    for (String statement : sql.split(";")) {
      if (!statement.isBlank()) {
        jdbc.execute(statement);
      }
    }
    var originalContract =
        json.readTree(
            jdbc.queryForObject(
                "select program_snapshot::text from collection_enrollments where id=?",
                String.class,
                e.getId()));
    var fallback =
        json.readTree(
            jdbc.queryForObject(
                "select program_snapshot::text from collection_enrollments where id=?",
                String.class,
                manual.getId()));
    assertThat(originalContract.path("courses").get(0).path("courseId").asText())
        .isEqualTo(original.getId().toString());
    assertThat(fallback.path("courses").get(0).path("courseId").asText())
        .isEqualTo(added.getId().toString());
    assertThat(originalContract.path("requiredCredits").isNull()).isTrue();
  }
}
