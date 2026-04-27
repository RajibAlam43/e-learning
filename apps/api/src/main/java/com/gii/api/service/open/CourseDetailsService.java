package com.gii.api.service.open;

import com.gii.api.model.response.*;
import com.gii.common.entity.course.*;
import com.gii.common.entity.user.User;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseDetailsService {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final LessonRepository lessonRepository;

    public CourseDetailsResponse execute(String slug) {
        Course course = courseRepository.findBySlugAndStatus(slug, PublishStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<CourseSection> sections =
                courseSectionRepository.findByCourseIdOrderByPositionAsc(course.getId());

        List<Lesson> lessons =
                lessonRepository.findByCourseIdAndStatusOrderByPositionAsc(
                        course.getId(),
                        PublishStatus.PUBLISHED
                );

        List<CourseSectionResponse> sectionResponses = sections.stream()
                .map(section -> toSectionResponse(section, lessons))
                .toList();

        return CourseDetailsResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .shortDescription(course.getShortDescription())
                .description(course.getDescription())
                .language(course.getLanguage())
                .level(course.getLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .priceBdt(course.getPriceBdt())
                .publishedAt(course.getPublishedAt())
                .sections(sectionResponses)
                .build();
    }

    private CourseSectionResponse toSectionResponse(
            CourseSection section,
            List<Lesson> allLessons
    ) {
        List<LessonSummaryResponse> lessons = allLessons.stream()
                .filter(lesson -> lesson.getSection() != null)
                .filter(lesson -> lesson.getSection().getId().equals(section.getId()))
                .map(this::toLessonSummaryResponse)
                .toList();

        return CourseSectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .position(section.getPosition())
                .lessons(lessons)
                .build();
    }

    private LessonSummaryResponse toLessonSummaryResponse(Lesson lesson) {
        MediaAsset media = lesson.getPrimaryMediaAsset();

        LessonVideoResponse video = null;

        if (Boolean.TRUE.equals(lesson.getIsFree()) && media != null) {
            video = LessonVideoResponse.builder()
                    .provider(media.getProvider())
                    .sourceId(media.getProviderAssetId()) // YouTube ID
                    .build();
        }

        return LessonSummaryResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .slug(lesson.getSlug())
                .position(lesson.getPosition())
                .lessonType(lesson.getLessonType())
                .isPreviewFree(lesson.getIsFree())
                .video(video)
                .build();
    }

    private CategoryResponse toCategoryResponse(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }

    private InstructorSummaryResponse toInstructorSummaryResponse(User instructor) {
        return InstructorSummaryResponse.builder()
                .id(instructor.getId())
                .fullName(instructor.getFullName())
                .build();
    }
}