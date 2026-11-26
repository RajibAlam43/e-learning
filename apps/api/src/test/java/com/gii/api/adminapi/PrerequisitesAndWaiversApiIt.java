package com.gii.api.adminapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.api.model.request.admin.CreateWaiverRequest;
import com.gii.api.model.request.admin.ReviewWaiverRequest;
import com.gii.api.model.request.payment.CreateCheckoutOrderItemRequest;
import com.gii.api.model.request.payment.CreateCheckoutOrderRequest;
import com.gii.api.service.academic.CoursePrerequisiteManagementService;
import com.gii.api.service.academic.CourseWaiverService;
import com.gii.api.service.academic.WaiverEvidenceService;
import com.gii.api.service.payment.PendingCartOrderService;
import com.gii.api.service.payment.PendingOrderEligibilityService;
import com.gii.api.service.progress.CollectionEnrollmentCompletionService;
import com.gii.api.service.storage.R2ObjectStorageService;
import com.gii.common.entity.collection.CollectionEnrollment;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.user.User;
import com.gii.common.enums.AcademicAchievementSource;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.OrderItemType;
import com.gii.common.enums.OrderStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.WaiverStatus;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.service.payment.CourseEnrollmentPolicyService;
import com.gii.common.service.payment.CoursePrerequisiteEligibilityService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@Transactional
class PrerequisitesAndWaiversApiIt extends AbstractAdminApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired CoursePrerequisiteManagementService rules;
  @Autowired CourseWaiverService waivers;
  @Autowired CourseEnrollmentPolicyService policy;
  @Autowired CoursePrerequisiteEligibilityService eligibility;
  @Autowired PendingCartOrderService checkout;
  @Autowired PendingOrderEligibilityService pending;
  @Autowired PaidOrderEnrollmentService paid;
  @Autowired AcademicAchievementRepository awards;
  @Autowired CollectionEnrollmentRepository collections;
  @Autowired CollectionEnrollmentCompletionService collectionCompletion;
  @Autowired WaiverEvidenceService evidence;
  @Autowired JdbcTemplate jdbc;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
  @MockitoBean R2ObjectStorageService storage;
  final ObjectMapper json = new ObjectMapper();

  Authentication student(User user) {
    return new UsernamePasswordAuthenticationToken(
        user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
  }

  Course published(String name, User owner) {
    var c = course(name, name.toLowerCase().replace(' ', '-'), owner, PublishStatus.PUBLISHED);
    c.setIsFree(false);
    c.setPriceBdt(BigDecimal.valueOf(500));
    return courseRepository.saveAndFlush(c);
  }

  String reason = "Completed equivalent prior studies";

  @Test
  void graphRejectsSelfLinksDuplicatesAndIndirectCyclesAndSharesRulesBetweenCohorts()
      throws Exception {
    var admin = user("Admin", "graph-admin@example.com");
    var a = published("Graph A", admin);
    var b = published("Graph B", admin);
    var c = published("Graph C", admin);
    rules.save(a.getId(), List.of(b.getTemplate().getId()));
    rules.save(b.getId(), List.of(c.getTemplate().getId()));
    assertThatThrownBy(() -> rules.save(c.getId(), List.of(a.getTemplate().getId())))
        .hasMessageContaining("cycle");
    mvc.perform(
            put("/admin/courses/" + a.getId() + "/prerequisites")
                .with(authentication(adminAuth(admin.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseTemplateIds\":[\"" + a.getTemplate().getId() + "\"]}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/admin/courses/" + a.getId() + "/prerequisites")
                .with(authentication(student(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseTemplateIds\":[]}"))
        .andExpect(status().isForbidden());
    var another =
        courseRepository.saveAndFlush(
            Course.builder()
                .template(a.getTemplate())
                .slug("another-graph-cohort")
                .createdBy(admin)
                .build());
    assertThat(rules.get(another.getId()).requiredCourses()).hasSize(1);
    rules.save(another.getId(), List.of());
    assertThat(rules.get(a.getId()).requiredCourses()).isEmpty();
  }

  @Test
  void purchaseAndFreeEnrollmentCannotBypassRequirementsAndApprovalGrantsCredits()
      throws Exception {
    var admin = user("Admin", "waiver-admin@example.com");
    var learner = user("Learner", "waiver-student@example.com");
    var required = published("Required", admin);
    var target = published("Advanced", admin);
    rules.save(target.getId(), List.of(required.getTemplate().getId()));
    var request =
        new CreateCheckoutOrderRequest(
            List.of(
                new CreateCheckoutOrderItemRequest(OrderItemType.COURSE, target.getId(), null)));
    assertThatThrownBy(() -> checkout.execute(request, student(learner)))
        .hasMessageContaining("credit waiver");
    target.setIsFree(true);
    target.setPriceBdt(BigDecimal.ZERO);
    courseRepository.saveAndFlush(target);
    mvc.perform(
            post("/student/courses/" + target.getId() + "/enroll")
                .with(authentication(student(learner))))
        .andExpect(status().isConflict());
    mvc.perform(
            get("/student/courses/" + target.getId() + "/eligibility")
                .with(authentication(student(learner))))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[0].courseTemplateId").value(required.getTemplate().getId().toString()));
    final var w =
        waivers.create(
            learner.getId(), new CreateWaiverRequest(required.getTemplate().getId(), reason, null));
    var decision =
        new ReviewWaiverRequest(WaiverStatus.APPROVED, "Equivalent curriculum verified", 3, null);
    var approved = waivers.review(w.id(), admin.getId(), decision);
    waivers.review(w.id(), admin.getId(), decision);
    assertThat(approved.awardedCredits()).isEqualTo(3);
    assertThat(awards.findByUserIdOrderByAchievedAtDesc(learner.getId()))
        .hasSize(1)
        .allSatisfy(a -> assertThat(a.getSource()).isEqualTo(AcademicAchievementSource.EXEMPTION));
    assertThat(enrollmentRepository.findByUserIdAndCourseId(learner.getId(), required.getId()))
        .isEmpty();
    mvc.perform(
            post("/student/courses/" + target.getId() + "/enroll")
                .with(authentication(student(learner))))
        .andExpect(status().isNoContent());
    assertThat(eligibility.missing(learner.getId(), target.getTemplate().getId())).isEmpty();
  }

  @Test
  void paidOrderRetainsCheckoutEligibilityWhenRulesChange() {
    var admin = user("Admin", "proof-admin@example.com");
    var learner = user("Learner", "proof-student@example.com");
    var a = published("Proof Course", admin);
    var required = published("Later Requirement", admin);
    var request =
        new CreateCheckoutOrderRequest(
            List.of(new CreateCheckoutOrderItemRequest(OrderItemType.COURSE, a.getId(), null)));
    var order = checkout.execute(request, student(learner));
    rules.save(a.getId(), List.of(required.getTemplate().getId()));
    assertThat(checkout.execute(request, student(learner)).orderId()).isEqualTo(order.orderId());
    var entity = orderRepository.findById(order.orderId()).orElseThrow();
    pending.validate(entity);
    entity.setStatus(OrderStatus.PAID);
    orderRepository.saveAndFlush(entity);
    paid.grant(entity.getId());
    assertThat(enrollmentRepository.findByUserIdAndCourseId(learner.getId(), a.getId()))
        .isPresent();
    var other = user("Other", "proof-other@example.com");
    assertThatThrownBy(
            () -> policy.validateCheckout(a, other.getId(), Instant.now(), entity.getId()))
        .hasMessageContaining("credit waiver");
  }

  @Test
  void historicalCompletionOfAnotherCohortSatisfiesPrerequisiteWithoutCreditBackfill() {
    var admin = user("Admin", "history-admin@example.com");
    var learner = user("Learner", "history-student@example.com");
    var required = published("History Required", admin);
    var target = published("History Advanced", admin);
    var second =
        courseRepository.saveAndFlush(
            Course.builder()
                .template(required.getTemplate())
                .slug("history-other-cohort")
                .createdBy(admin)
                .build());
    var enrollment = enrollment(learner, second, EnrollmentStatus.ACTIVE);
    enrollment.setCompletedAt(Instant.now());
    enrollmentRepository.saveAndFlush(enrollment);
    rules.save(target.getId(), List.of(required.getTemplate().getId()));
    assertThat(eligibility.missing(learner.getId(), target.getTemplate().getId())).isEmpty();
    assertThat(awards.findByUserIdOrderByAchievedAtDesc(learner.getId())).isEmpty();
  }

  @Test
  void waiverAccessDuplicateRejectionResubmissionAndCreditChangeAreProtected() throws Exception {
    var admin = user("Admin", "review-admin@example.com");
    var learner = user("Learner", "review-student@example.com");
    var other = user("Other", "review-other@example.com");
    var c = published("Review Course", admin);
    final var w =
        waivers.create(
            learner.getId(), new CreateWaiverRequest(c.getTemplate().getId(), reason, null));
    assertThatThrownBy(() ->
            waivers.create(
                learner.getId(), new CreateWaiverRequest(c.getTemplate().getId(), reason, null)))
        .hasMessageContaining("already pending");
    mvc.perform(get("/student/waivers").with(authentication(student(other))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            get("/student/waivers/" + w.id() + "/evidence").with(authentication(student(other))))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/admin/waivers/" + w.id() + "/review")
                .with(authentication(student(learner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"reason\":\"yes\",\"expectedCredits\":3}"))
        .andExpect(status().isForbidden());
    waivers.review(
        w.id(),
        admin.getId(),
        new ReviewWaiverRequest(WaiverStatus.REJECTED, "Please provide a transcript", 3, null));
    assertThat(awards.findByUserIdOrderByAchievedAtDesc(learner.getId())).isEmpty();
    var again =
        waivers.create(
            learner.getId(),
            new CreateWaiverRequest(c.getTemplate().getId(), "Transcript supplied", null));
    c.setCredits(4);
    courseRepository.saveAndFlush(c);
    assertThatThrownBy(() ->
            waivers.review(
                again.id(),
                admin.getId(),
                new ReviewWaiverRequest(WaiverStatus.APPROVED, "Verified", 3, null)))
        .hasMessageContaining("credits changed");
  }

  @Test
  void approvalDoesNotDuplicateExistingCompletionCredits() {
    var admin = user("Admin", "duplicate-admin@example.com");
    var learner = user("Learner", "duplicate-student@example.com");
    var c = published("Duplicate Course", admin);
    final var w =
        waivers.create(
            learner.getId(), new CreateWaiverRequest(c.getTemplate().getId(), reason, null));
    var e = enrollment(learner, c, EnrollmentStatus.ACTIVE);
    e.setCompletedAt(Instant.now());
    enrollmentRepository.saveAndFlush(e);
    awards.awardCompletion(e.getId());
    var result =
        waivers.review(
            w.id(),
            admin.getId(),
            new ReviewWaiverRequest(WaiverStatus.APPROVED, "Verified", 3, null));
    assertThat(result.awardedCredits()).isEqualTo(3);
    assertThat(awards.findByUserIdOrderByAchievedAtDesc(learner.getId()))
        .hasSize(1)
        .allSatisfy(a -> assertThat(a.getSource()).isEqualTo(AcademicAchievementSource.COMPLETION));
  }

  @Test
  void approvedWaiverSatisfiesMandatoryCollectionCourseWithoutFabricatedEnrollment() {
    var admin = user("Admin", "collection-waiver-admin@example.com");
    var learner = user("Learner", "collection-waiver-student@example.com");
    var c = published("Mandatory Course", admin);
    var program = collection("Diploma", "waiver-diploma", admin, PublishStatus.PUBLISHED);
    collectionCourse(program, c, 1, true);
    var ce =
        collections.saveAndFlush(
            CollectionEnrollment.builder()
                .user(learner)
                .collection(program)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .build());
    collectionCompletion.refresh(learner.getId(), program.getId());
    assertThat(ce.getCompletedAt()).isNull();
    final var w =
        waivers.create(
            learner.getId(), new CreateWaiverRequest(c.getTemplate().getId(), reason, null));
    waivers.review(
        w.id(), admin.getId(), new ReviewWaiverRequest(WaiverStatus.APPROVED, "Verified", 3, null));
    assertThat(ce.getCompletedAt()).isNotNull();
    assertThat(enrollmentRepository.findByUserIdAndCourseId(learner.getId(), c.getId())).isEmpty();
  }

  @Test
  void evidenceMustBePrivateOwnedAndHaveAllowedMetadata() {
    var learner = user("Evidence", "evidence-owner@example.com");
    var other = user("Other", "evidence-other@example.com");
    String key = "waiver-evidence/" + learner.getId() + "/" + UUID.randomUUID() + ".pdf";
    when(storage.head(key))
        .thenReturn(
            HeadObjectResponse.builder()
                .contentLength(100L)
                .contentType("application/pdf")
                .build());
    evidence.validate(learner.getId(), key);
    assertThatThrownBy(() -> evidence.validate(other.getId(), key))
        .hasMessageContaining("Invalid supporting file");
    assertThatThrownBy(() -> evidence.validate(learner.getId(), "lesson-resources/private.pdf"))
        .hasMessageContaining("Invalid supporting file");
    when(storage.head(key))
        .thenReturn(
            HeadObjectResponse.builder()
                .contentLength(10485761L)
                .contentType("application/pdf")
                .build());
    assertThatThrownBy(() -> evidence.validate(learner.getId(), key)).hasMessageContaining("size");
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void concurrentGraphEditsCannotCreateCycle() throws Exception {
    try {
      var ids =
          new org.springframework.transaction.support.TransactionTemplate(transactions)
              .execute(tx -> {
                var admin = user("Graph Race", "graph-race@example.com");
                var a = published("Graph Race A", admin);
                var b = published("Graph Race B", admin);
                return List.of(
                    a.getId(), a.getTemplate().getId(), b.getId(), b.getTemplate().getId());
              });
      var start = new java.util.concurrent.CountDownLatch(1);
      try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
        var first =
            executor.submit(() -> {
              start.await();
              return saveGraphEdge(ids.get(0), ids.get(3));
            });
        var second =
            executor.submit(() -> {
              start.await();
              return saveGraphEdge(ids.get(2), ids.get(1));
            });
        start.countDown();
        assertThat(
                first.get(10, java.util.concurrent.TimeUnit.SECONDS)
                    ^ second.get(10, java.util.concurrent.TimeUnit.SECONDS))
            .isTrue();
      }
    } finally {
      cleanupAdminData();
    }
  }

  private boolean saveGraphEdge(UUID courseId, UUID required) {
    try {
      rules.save(courseId, List.of(required));
      return true;
    } catch (org.springframework.web.server.ResponseStatusException exception) {
      assertThat(exception.getStatusCode().value()).isEqualTo(409);
      return false;
    }
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void concurrentWaiverAndCompletionKeepExactlyOneCreditAward() throws Exception {
    try {
      var setup =
          new org.springframework.transaction.support.TransactionTemplate(transactions)
              .execute(tx -> {
                var admin = user("Credit Race Admin", "waiver-race-admin@example.com");
                var learner = user("Credit Race Learner", "waiver-race-student@example.com");
                var c = published("Waiver Race Course", admin);
                var request =
                    waivers.create(
                        learner.getId(),
                        new CreateWaiverRequest(c.getTemplate().getId(), reason, null));
                var enrollment = enrollment(learner, c, EnrollmentStatus.ACTIVE);
                enrollment.setCompletedAt(Instant.now());
                enrollmentRepository.saveAndFlush(enrollment);
                return List.of(admin.getId(), learner.getId(), request.id(), enrollment.getId());
              });
      var start = new java.util.concurrent.CountDownLatch(1);
      try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
        var first =
            executor.submit(() -> {
              start.await();
              return waivers.review(
                  setup.get(2),
                  setup.get(0),
                  new ReviewWaiverRequest(
                      WaiverStatus.APPROVED, "Verified equivalent study", 3, null));
            });
        var second =
            executor.submit(() -> {
              start.await();
              return new org.springframework.transaction.support.TransactionTemplate(transactions)
                  .execute(tx -> awards.awardCompletion(setup.get(3)));
            });
        start.countDown();
        first.get(10, java.util.concurrent.TimeUnit.SECONDS);
        second.get(10, java.util.concurrent.TimeUnit.SECONDS);
      }
      assertThat(awards.findByUserIdOrderByAchievedAtDesc(setup.get(1))).hasSize(1);
    } finally {
      awards.deleteAll();
      cleanupAdminData();
    }
  }
}
