package com.gii.api.service.academic;

import com.gii.api.model.request.admin.SaveSemesterRequest;
import com.gii.api.model.response.academic.SemesterResponse;
import com.gii.common.entity.academic.Semester;
import com.gii.common.entity.course.Course;
import com.gii.common.enums.StudyMode;
import com.gii.common.repository.academic.SemesterRepository;
import com.gii.common.repository.course.CourseRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class SemesterService {
  private final SemesterRepository semesters;
  private final CourseRepository courses;

  @Transactional(readOnly = true)
  public List<SemesterResponse> list() {
    return semesters.findAllByOrderByStartsAtDesc().stream().map(SemesterResponse::from).toList();
  }

  public SemesterResponse save(UUID id, SaveSemesterRequest request) {
    if (!request.endsAt().isAfter(request.startsAt())) {
      throw badRequest("Semester end must be after its start");
    }
    if (!request.registrationEndsAt().isAfter(request.registrationStartsAt())
        || request.registrationEndsAt().isAfter(request.endsAt())) {
      throw badRequest("Registration must end after it opens and no later than the semester end");
    }
    String code = request.code().trim().toUpperCase(Locale.ROOT);
    Semester semester =
        id == null
            ? Semester.builder().build()
            : semesters
                .findByIdForUpdate(id)
                .orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Semester not found"));
    if (semesters.existsByCodeAndIdNot(code, id == null ? new UUID(0, 0) : id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Semester code is already in use");
    }
    semester.setCode(code);
    semester.setName(request.name().trim());
    semester.setStartsAt(request.startsAt());
    semester.setEndsAt(request.endsAt());
    semester.setRegistrationStartsAt(request.registrationStartsAt());
    semester.setRegistrationEndsAt(request.registrationEndsAt());
    if (id != null) {
      courses.findBySemesterId(id).forEach(course -> validateDates(course, semester));
    }
    return SemesterResponse.from(semesters.saveAndFlush(semester));
  }

  public void assignNewOffering(Course course, UUID semesterId) {
    if (semesterId == null && course.getStudyMode() == StudyMode.COHORT_BASED) {
      throw badRequest("Select a semester for a cohort-based course");
    }
    assign(course, semesterId);
  }

  public void assign(Course course, UUID semesterId) {
    if (semesterId == null) {
      course.setSemester(null);
      return;
    }
    Semester semester =
        semesters
            .findByIdForUpdate(semesterId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Semester not found"));
    course.setSemester(semester);
    if (course.getStartsAt() == null) {
      course.setStartsAt(semester.getStartsAt());
    }
    if (course.getEndsAt() == null) {
      course.setEndsAt(semester.getEndsAt());
    }
    if (course.getEnrollmentStartsAt() == null) {
      course.setEnrollmentStartsAt(semester.getRegistrationStartsAt());
    }
    if (course.getEnrollmentEndsAt() == null) {
      course.setEnrollmentEndsAt(semester.getRegistrationEndsAt());
    }
    validateDates(course, semester);
  }

  public void validate(Course course) {
    if (course.getSemester() != null) {
      assign(course, course.getSemester().getId());
    }
  }

  private void validateDates(Course course, Semester semester) {
    if (outside(course.getStartsAt(), semester.getStartsAt(), semester.getEndsAt())
        || outside(course.getEndsAt(), semester.getStartsAt(), semester.getEndsAt())) {
      throw badRequest("Course teaching dates must be within its semester");
    }
    if (outside(
            course.getEnrollmentStartsAt(),
            semester.getRegistrationStartsAt(),
            semester.getRegistrationEndsAt())
        || outside(
            course.getEnrollmentEndsAt(),
            semester.getRegistrationStartsAt(),
            semester.getRegistrationEndsAt())) {
      throw badRequest("Course registration dates must be within semester registration dates");
    }
  }

  private boolean outside(Instant value, Instant start, Instant end) {
    return value != null && (value.isBefore(start) || value.isAfter(end));
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
