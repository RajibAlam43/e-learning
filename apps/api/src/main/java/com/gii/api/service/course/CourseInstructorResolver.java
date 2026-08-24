package com.gii.api.service.course;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.user.User;
import com.gii.common.enums.InstructorRole;
import com.gii.common.repository.course.CourseInstructorRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseInstructorResolver {

  private final CourseInstructorRepository courseInstructorRepository;

  public User primaryInstructor(Course course) {
    List<CourseInstructor> instructors = courseInstructorRepository.findByCourseId(course.getId());
    return instructors.stream()
        .filter(instructor -> instructor.getRole() == InstructorRole.PRIMARY)
        .findFirst()
        .or(() -> instructors.stream().findFirst())
        .map(CourseInstructor::getInstructor)
        .orElse(null);
  }
}
