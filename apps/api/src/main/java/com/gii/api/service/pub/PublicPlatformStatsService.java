package com.gii.api.service.pub;

import com.gii.api.model.response.PublicPlatformStatsResponse;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.collection.CollectionRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import com.gii.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicPlatformStatsService {

  private static final String STUDENT_ROLE = "STUDENT";

  private final UserRepository userRepository;
  private final CourseRepository courseRepository;
  private final CollectionRepository collectionRepository;
  private final InstructorProfileRepository instructorProfileRepository;

  public PublicPlatformStatsResponse execute() {
    return PublicPlatformStatsResponse.builder()
        .students(userRepository.countByRoleNameAndStatus(STUDENT_ROLE, UserStatus.ACTIVE))
        .courses(courseRepository.countByStatus(PublishStatus.PUBLISHED))
        .programs(collectionRepository.countByStatus(PublishStatus.PUBLISHED))
        .instructors(instructorProfileRepository.countPublicByUserStatus(UserStatus.ACTIVE))
        .build();
  }
}
