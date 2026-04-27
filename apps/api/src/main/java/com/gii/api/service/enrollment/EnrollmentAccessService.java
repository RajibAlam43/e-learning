package com.gii.api.service.enrollment;

import com.gii.common.entity.course.Lesson;
import com.gii.common.enums.EnrollmentStatus;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentAccessService {

    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;

    public void verifyCanAccessLesson(UUID userId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (lesson.getIsFree()) {
            return;
        }

        UUID courseId = lesson.getCourse().getId();

        boolean hasAccess = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                userId,
                courseId,
                EnrollmentStatus.ACTIVE
        );

        if (!hasAccess) {
            throw new RuntimeException("You do not have access to this lesson");
        }
    }
}
