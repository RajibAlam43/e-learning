package com.gii.api.service.student;

import com.gii.api.model.response.student.StudentCompletedLessonsResponse;
import com.gii.api.service.enrollment.CurrentUserService;
import com.gii.common.repository.enrollment.LessonProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCompletedLessonsService {

  private final CurrentUserService currentUserService;
  private final LessonProgressRepository lessonProgressRepository;

  public StudentCompletedLessonsResponse execute(Authentication authentication) {
    long total =
        lessonProgressRepository.countByUserIdAndCompletedAtIsNotNull(
            currentUserService.getCurrentUserId(authentication));
    return StudentCompletedLessonsResponse.builder().totalCompletedLessons(total).build();
  }
}
