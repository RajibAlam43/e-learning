package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentLiveClassSummaryResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.entity.enrollment.Enrollment;
import com.gii.common.entity.live.LiveClass;
import com.gii.common.entity.live.LiveClassRegistrant;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.live.LiveClassRegistrantRepository;
import com.gii.common.repository.live.LiveClassRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseLiveClassesService {

  private final CurrentUserService currentUserService;
  private final EnrollmentRepository enrollmentRepository;
  private final LiveClassRepository liveClassRepository;
  private final LiveClassRegistrantRepository registrantRepository;
  private final StudentUpcomingLiveClasses liveClassesMapper;

  public List<StudentLiveClassSummaryResponse> execute(
      UUID courseId, Authentication authentication) {
    UUID userId = currentUserService.getCurrentUserId(authentication);
    Enrollment enrollment =
        enrollmentRepository
            .findByUserIdAndCourseId(userId, courseId)
            .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

    List<LiveClass> liveClasses =
        liveClassRepository.findByCourseIdOrderByStartsAtAsc(enrollment.getCourse().getId());
    if (liveClasses.isEmpty()) {
      return List.of();
    }

    Map<UUID, LiveClassRegistrant> registrantByClassId =
        registrantRepository
            .findByUserIdAndLiveClassIds(
                userId, liveClasses.stream().map(LiveClass::getId).toList())
            .stream()
            .collect(Collectors.toMap(r -> r.getLiveClass().getId(), r -> r));

    return liveClasses.stream()
        .map(
            liveClass ->
                liveClassesMapper.toSummary(liveClass, registrantByClassId.get(liveClass.getId())))
        .toList();
  }
}
