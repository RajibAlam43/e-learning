package com.gii.api.service.open;

import com.gii.common.entity.course.Course;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.api.model.response.CourseSummaryResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.common.enums.PublishStatus;
import com.gii.common.repository.course.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllCoursesService {

    private final CourseRepository courseRepository;

    public PageResponse<CourseSummaryResponse> execute(UUID categoryId, CourseLevel level, CourseLanguage language, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "publishedAt")
        );

        Specification<Course> spec = Specification
                .where(CourseSpecifications.hasStatus(PublishStatus.PUBLISHED))
                .and(CourseSpecifications.hasCategory(categoryId))
                .and(CourseSpecifications.hasLevel(level))
                .and(CourseSpecifications.hasLanguage(language));

        Page<Course> coursePage = courseRepository.findAll(spec, sortedPageable);

        List<CourseSummaryResponse> courses = coursePage.getContent()
                .stream()
                .map(this::toCourseSummaryResponse)
                .toList();

        return PageResponse.<CourseSummaryResponse>builder()
                .content(courses)
                .page(coursePage.getNumber())
                .size(coursePage.getSize())
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .build();
    }

    private CourseSummaryResponse toCourseSummaryResponse(Course course) {
        return CourseSummaryResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .shortDescription(course.getShortDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .priceBdt(course.getPriceBdt())
                .level(course.getLevel())
                .language(course.getLanguage())
                .publishedAt(course.getPublishedAt())
                .build();
    }
}
