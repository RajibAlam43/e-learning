package com.gii.api.service.academic;

import com.gii.api.model.response.academic.ProgramOfferingResponse;
import com.gii.api.model.response.academic.SemesterResponse;
import com.gii.api.model.response.academic.StudentProgramCurriculumResponse;
import com.gii.api.service.collection.PurchasedCollectionCoursesService;
import com.gii.api.service.progress.ProgramCreditProgressService;
import com.gii.common.entity.course.Course;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.ProgramRegistrationStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.academic.AcademicAchievementRepository;
import com.gii.common.repository.academic.ProgramCourseRegistrationRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.service.payment.CourseEnrollmentPolicyService;
import com.gii.common.service.payment.CoursePrerequisiteEligibilityService;
import com.gii.common.service.payment.EnrollmentPolicyViolationException;
import com.gii.common.service.payment.ProgramCourseAccessService;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentProgramCurriculumService {
  private final CollectionEnrollmentRepository enrollments;
  private final EnrollmentRepository courseEnrollments;
  private final PurchasedCollectionCoursesService courses;
  private final CourseRepository offerings;
  private final ProgramCreditProgressService progress;
  private final ProgramCourseRegistrationRepository registrations;
  private final AcademicAchievementRepository achievements;
  private final ProgramRegistrationAllowanceService allowance;
  private final ProgramCourseRegistrationService registrationService;
  private final CourseEnrollmentPolicyService policy;
  private final CoursePrerequisiteEligibilityService prerequisites;
  private final OrderItemRepository orders;

  public StudentProgramCurriculumResponse get(UUID userId, UUID id) {
    var e =
        enrollments
            .findByUserIdAndCollectionId(userId, id)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Program enrollment not found"));
    Instant now = Instant.now();
    boolean active = ProgramCourseRegistrationService.active(e, now);
    var selected =
        registrations.findByProgramEnrollmentIdAndStatus(
            e.getId(), ProgramRegistrationStatus.ACTIVE);
    var selectedByTemplate =
        selected.stream()
            .collect(java.util.stream.Collectors.toMap(r -> r.getCourseTemplateId(), r -> r));
    var budget = allowance.get(e, selected);
    var enrolled =
        courseEnrollments.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE).stream()
            .filter(c -> c.getExpiresAt() == null || c.getExpiresAt().isAfter(now))
            .toList();
    var awards = new HashMap<UUID, Integer>();
    achievements
        .findByUserIdOrderByAchievedAtDesc(userId)
        .forEach(a -> awards.put(a.getCourseTemplateId(), a.getCredits()));
    var available =
        courses.resolveItems(e).stream()
            .map(c -> {
              var representative = c.course();
              UUID template = representative.getTemplate().getId();
              int admissionCredits =
                  com.gii.common.service.payment.ProgramSnapshotService.courseCredits(
                      e, representative);
              var chosen = selectedByTemplate.get(template);
              var owned =
                  enrolled.stream()
                      .filter(v -> v.getCourse().getTemplate().getId().equals(template))
                      .findFirst()
                      .orElse(null);
              var missing = prerequisites.missing(userId, template);
              var choices =
                  offerings.findByTemplateId(template).stream()
                      .filter(o -> o.getStatus() == PublishStatus.PUBLISHED)
                      .sorted(
                          Comparator.comparing(
                                  Course::getStartsAt,
                                  Comparator.nullsLast(Comparator.naturalOrder()))
                              .thenComparing(Course::getSlug))
                      .map(o -> {
                        String blocked = null;
                        if (!active) {
                          blocked = "Program access is inactive or has expired";
                        } else if (chosen != null) {
                          blocked =
                              "A cohort is already selected; contact an administrator to change it";
                        } else if (awards.containsKey(template)) {
                          blocked = "Academic credit already awarded; registration is not required";
                        } else if (owned != null && owned.getCompletedAt() != null) {
                          blocked =
                              "This course is already completed; "
                                  + "request a credit waiver if its credits are missing";
                        } else if (owned != null && !owned.getCourse().getId().equals(o.getId())) {
                          blocked = "You are already enrolled in a cohort of this course";
                        } else if (!c.mandatory()
                            && budget.remaining() != null
                            && admissionCredits > budget.remaining()) {
                          blocked = "Insufficient elective credit allowance";
                        } else if (owned == null) {
                          try {
                            policy.validateCheckout(o, userId, now);
                          } catch (EnrollmentPolicyViolationException ex) {
                            blocked = ex.getMessage();
                          }
                        }
                        Long seats =
                            o.getCapacity() == null
                                ? null
                                : Math.max(
                                    0,
                                    o.getCapacity()
                                        - courseEnrollments.countAvailableSeatsInUse(
                                            o.getId(), EnrollmentStatus.ACTIVE, now)
                                        - orders.countPendingReservations(
                                            o.getId(), userId, now.minusSeconds(1800)));
                        return new ProgramOfferingResponse(
                            o.getId(),
                            o.getName() == null ? o.getSlug() : o.getName(),
                            o.getSlug(),
                            o.getStudyMode(),
                            o.getSemester() == null ? null : SemesterResponse.from(o.getSemester()),
                            o.getStartsAt(),
                            o.getEndsAt(),
                            o.getTimezone(),
                            admissionCredits,
                            ProgramCourseAccessService.earlier(
                                e.getExpiresAt(), policy.calculateExpiry(o, now)),
                            seats,
                            blocked == null,
                            blocked,
                            missing);
                      })
                      .toList();
              var display = owned == null ? representative : owned.getCourse();
              return new StudentProgramCurriculumResponse.AvailableProgramCourse(
                  display.getId(),
                  template,
                  representative.getTitleEn() != null && !representative.getTitleEn().isBlank()
                      ? representative.getTitleEn()
                      : representative.getTitle(),
                  display.getStatus() == PublishStatus.PUBLISHED ? display.getSlug() : null,
                  admissionCredits,
                  display.getStatus(),
                  c.mandatory(),
                  owned != null,
                  awards.get(template),
                  chosen == null ? null : registrationService.response(chosen),
                  choices);
            })
            .toList();
    return new StudentProgramCurriculumResponse(
        e.getProgramSnapshot(), progress.get(e), e.getExpiresAt(), active, budget, available);
  }
}
