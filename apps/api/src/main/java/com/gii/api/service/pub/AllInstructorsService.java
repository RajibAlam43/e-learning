package com.gii.api.service.pub;

import com.gii.api.model.response.InstructorSummaryResponse;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllInstructorsService {

  private final InstructorProfileRepository instructorProfileRepository;
  private final CourseInstructorRepository courseInstructorRepository;

  public List<InstructorSummaryResponse> execute() {
    List<InstructorProfile> profiles =
        instructorProfileRepository.findPublicByUserStatus(UserStatus.ACTIVE);
    if (profiles.isEmpty()) {
      return List.of();
    }

    List<UUID> instructorIds = profiles.stream().map(InstructorProfile::getUserId).toList();
    Map<UUID, Integer> publishedCourseCounts =
        toCountMap(
            courseInstructorRepository.countByInstructorIdsAndCourseStatus(
                instructorIds, PublishStatus.PUBLISHED));

    return profiles.stream()
        .map(profile -> toSummary(profile, publishedCourseCounts))
        .toList();
  }

  private InstructorSummaryResponse toSummary(
      InstructorProfile profile, Map<UUID, Integer> publishedCourseCounts) {
    return InstructorSummaryResponse.builder()
        .id(profile.getUserId())
        .slug(profile.getUserId().toString())
        .fullName(profile.getUser().getFullName())
        .avatarUrl(profile.getPhotoUrl())
        .shortBio(profile.getHeadline())
        .credentials(profile.getCredentialsText())
        .yearsExperience(profile.getYearsExperience())
        .publishedCoursesCount(publishedCourseCounts.getOrDefault(profile.getUserId(), 0))
        .build();
  }

  private Map<UUID, Integer> toCountMap(List<Object[]> rows) {
    Map<UUID, Integer> result = new HashMap<>();
    for (Object[] row : rows) {
      result.put((UUID) row[0], ((Long) row[1]).intValue());
    }
    return result;
  }
}
