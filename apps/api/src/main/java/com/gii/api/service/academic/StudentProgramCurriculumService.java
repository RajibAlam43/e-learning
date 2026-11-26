package com.gii.api.service.academic;

import com.gii.api.model.response.academic.StudentProgramCurriculumResponse;
import com.gii.api.service.collection.PurchasedCollectionCoursesService;
import com.gii.api.service.progress.ProgramCreditProgressService;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import java.time.Instant;
import java.util.HashSet;
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
  private final ProgramCreditProgressService progress;

  public StudentProgramCurriculumResponse get(UUID userId, UUID id) {
    var e =
        enrollments
            .findByUserIdAndCollectionId(userId, id)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Program enrollment not found"));
    boolean active =
        e.getStatus() == EnrollmentStatus.ACTIVE
            && (e.getExpiresAt() == null || e.getExpiresAt().isAfter(Instant.now()));
    var enrolled = new HashSet<UUID>();
    courseEnrollments.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE).stream()
        .filter(c -> c.getExpiresAt() == null || c.getExpiresAt().isAfter(Instant.now()))
        .forEach(c -> enrolled.add(c.getCourse().getId()));
    var available =
        courses.resolveItems(e).stream()
            .map(c -> {
              var course = c.course();
              return new StudentProgramCurriculumResponse.AvailableProgramCourse(
                  course.getId(),
                  course.getTemplate().getId(),
                  course.getTitleEn() != null && !course.getTitleEn().isBlank()
                      ? course.getTitleEn()
                      : course.getTitle(),
                  course.getStatus() == PublishStatus.PUBLISHED ? course.getSlug() : null,
                  course.getCredits(),
                  course.getStatus(),
                  c.mandatory(),
                  active && enrolled.contains(course.getId()));
            })
            .toList();
    return new StudentProgramCurriculumResponse(
        e.getProgramSnapshot(), progress.get(e), e.getExpiresAt(), active, available);
  }
}
